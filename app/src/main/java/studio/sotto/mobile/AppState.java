package studio.sotto.mobile;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.*;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class AppState {
    public interface Listener { void changed(); default void level(float value){} }
    public interface Destination { boolean insert(String text); }
    public interface Task { void run() throws Exception; }
    public interface Completion<T> { void done(T value); void failed(String message); }
    private static AppState instance;
    public final Store store; public final JSONObject rules; public final ApiClient api; public final GoogleAccount google;
    public final Handler main=new Handler(Looper.getMainLooper()); private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final CopyOnWriteArrayList<Listener> listeners=new CopyOnWriteArrayList<>(); private final Context context;
    public volatile String stage="idle",message="",owner=""; public JSONObject result; public long startedAt; public float meter;
    private long generation; private AudioCapture recorder; private SpeechRecognizer recognizer; private ApiClient.Cancellation cancellation; private Job retryJob,recordingJob; private String jobMode="precise";
    public static synchronized AppState get(Context context)throws Exception{if(instance==null)instance=new AppState(context);return instance;}
    private AppState(Context context)throws Exception{this.context=context.getApplicationContext();store=new Store(this.context);api=new ApiClient();google=new GoogleAccount(this.context,store,api);rules=new JSONObject(new String(Store.bytes(this.context.getAssets().open("dictation-rules.json"),100000),StandardCharsets.UTF_8));}
    public void add(Listener listener){listeners.addIfAbsent(listener);}public void remove(Listener listener){listeners.remove(listener);}
    public void notifyChanged(){for(Listener listener:listeners)listener.changed();}
    private void state(String stage,String message){this.stage=stage;this.message=message;notifyChanged();}
    public boolean busy(){return stage.equals("recording")||stage.equals("transcribing")||stage.equals("polishing")||stage.equals("inserting")||stage.equals("cancelling")||stage.equals("signing-in");}
    public boolean recording(){return stage.equals("recording");}
    public String mode(){return busy()?jobMode:store.mode();}
    public String modeDescription(){JSONObject info=rules.optJSONObject(store.mode());return info==null?"":info.optString("description");}
    public void mode(String value)throws Exception{if(busy())throw new Exception("Finish or cancel the current dictation first.");if(!DictationRules.validMode(value))throw new Exception("Choose one of the five writing modes.");store.settings.edit().putString("writingMode",value).apply();notifyChanged();}
    public void language(String value)throws Exception{if(busy()&&!recording())throw new Exception("Finish or cancel the current dictation first.");store.settings.edit().putString("languageOverride",value).apply();if(recordingJob!=null)recordingJob.language=value;notifyChanged();}
    public void startRecording(String origin,Destination destination)throws Exception{
        if(busy())throw new Exception("Finish or cancel the current operation first.");checkConnection();
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new Exception("Allow microphone access in the Sotto app, then return to your text field.");
        owner=origin;jobMode=store.mode();result=null;retryJob=null;startedAt=System.currentTimeMillis();final long token=++generation;Job job=new Job(null,null,0,destination);recordingJob=job;state("recording","");
        if("device".equals(store.settings.getString("recognition","provider"))){startDeviceRecognition(job,token);return;}
        recorder=new AudioCapture();
        try{recorder.start(new AudioCapture.Callback(){
            public void complete(byte[] wav,int seconds){main.post(()->{if(token==generation){recorder=null;job.wav=wav;job.seconds=seconds;process(job,null,token);}});}
            public void failed(String error){main.post(()->{if(token==generation){recorder=null;state("error",error);}});}
            public void level(float value){main.post(()->{if(token==generation){meter=value;for(Listener listener:listeners)listener.level(value);}});}
        });}catch(Exception e){recorder=null;state("error","The microphone is unavailable. Check Android permissions and your audio device.");throw e;}
    }
    private void startDeviceRecognition(Job job,long token){
        if(Build.VERSION.SDK_INT<31||!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)){state("error","On-device speech recognition is unavailable on this phone. Choose Selected provider in Settings.");return;}
        recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){}public void onBeginningOfSpeech(){}public void onBufferReceived(byte[] b){}public void onEvent(int type,Bundle b){}public void onPartialResults(Bundle b){}
            public void onRmsChanged(float rms){if(token==generation)for(Listener listener:listeners)listener.level(Math.max(0,Math.min(1,rms/10)));}
            public void onEndOfSpeech(){if(token==generation)state("transcribing","Finishing the on-device transcript.");}
            public void onError(int error){if(token!=generation)return;destroyRecognizer();state("error",error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS?"Allow microphone access in Android Settings.":error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT?"No clear speech was detected. Try recording again.":"The on-device recognizer could not finish. Check its language model or choose Selected provider in Settings.");}
            public void onResults(Bundle results){if(token!=generation)return;ArrayList<String> text=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);destroyRecognizer();if(text==null||text.isEmpty()||text.get(0).trim().isEmpty()){state("error","No clear speech was detected. Try again.");return;}job.text=text.get(0);process(job,null,token);}
        });
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false).putExtra(RecognizerIntent.EXTRA_LANGUAGE,store.settings.getString("inputLanguage",Locale.getDefault().toLanguageTag()));
        try{recognizer.startListening(intent);}catch(Exception error){destroyRecognizer();state("error","The on-device recognizer could not start. Choose Selected provider in Settings.");}
    }
    private void destroyRecognizer(){if(recognizer!=null){recognizer.destroy();recognizer=null;}}
    public void stopRecording(){if(!recording())return;state("transcribing","");if(recorder!=null)recorder.stop();else if(recognizer!=null)recognizer.stopListening();}
    public void cancel(){if(stage.equals("inserting"))return;if((stage.equals("transcribing")||stage.equals("polishing")||stage.equals("cancelling"))&&recorder==null&&recognizer==null&&cancellation!=null){cancellation.cancel();state("cancelling","Cancelling this request…");return;}++generation;if(recorder!=null){recorder.cancel();recorder=null;}if(recognizer!=null){recognizer.cancel();destroyRecognizer();}if(cancellation!=null)cancellation.cancel();google.cancelLogin();state("cancelled","Nothing was inserted.");owner="";}
    public void text(String text)throws Exception{if(busy())throw new Exception("Finish or cancel your current dictation first.");checkConnection();if(text==null||text.trim().isEmpty()||text.length()>60000)throw new Exception("Enter up to 60,000 characters to polish.");jobMode=store.mode();result=null;owner="app";process(new Job(null,text.trim(),0,null),null,++generation);}
    public void retry(String id)throws Exception{
        if(busy())throw new Exception("Finish or cancel your current dictation first.");checkConnection();Job job;
        if(id==null){if(retryJob==null)throw new Exception("No recording is available to retry.");job=new Job(retryJob.wav,retryJob.text,retryJob.seconds,null);}
        else{JSONObject entry=store.entry(id);if(entry==null)throw new Exception("This history entry no longer exists.");job=new Job(!entry.optString("audio").isEmpty()?store.readAudio(entry.getString("audio")):null,entry.optString("transcript",null),entry.optInt("duration"),null);if(job.wav==null&&(job.text==null||job.text.isEmpty()))throw new Exception("No audio or transcript was kept for this entry.");}
        jobMode=store.mode();owner="app";result=null;process(job,id,++generation);
    }
    public boolean canRetry(){return retryJob!=null&&!busy();}
    private void checkConnection()throws Exception{JSONObject c=store.active();if(c==null||c.optString("model").isEmpty())throw new Exception("Add a connection and select a model in Connections before dictating.");}
    private void process(Job job,String existingId,long token){
        recordingJob=null;
        final JSONObject selected=store.active();if(selected==null){state("error","Choose a connection first.");return;}
        final JSONObject connection;try{connection=new JSONObject(selected.toString());}catch(Exception e){state("error","Could not load this connection.");return;}
        final String id=existingId==null?UUID.randomUUID().toString():existingId;final boolean history=store.settings.getBoolean("saveHistory",true),keepAudio=store.settings.getBoolean("keepAudio",false),autoInsert=store.settings.getBoolean("autoPaste",true);
        retryJob=job;jobMode=job.mode;state("transcribing","");cancellation=new ApiClient.Cancellation();final ApiClient.Cancellation cancel=cancellation;
        worker.execute(()->{String audio="";JSONObject entry=new JSONObject();
            try{
                if(job.wav!=null&&(history||existingId!=null))audio=store.saveAudio(id,job.wav);
                entry.put("id",id).put("createdAt",existingId!=null&&store.entry(id)!=null?store.entry(id).optLong("createdAt",System.currentTimeMillis()):System.currentTimeMillis()).put("duration",job.seconds).put("connection",connection.optString("name")).put("model",connection.optString("model")).put("writingMode",job.mode).put("language",job.language.isEmpty()?"English":job.language).put("transcript",job.text==null?"":job.text).put("text","").put("status","processing").put("audio",audio);
                if(history||existingId!=null)store.putHistory(entry);main.post(()->{if(token==generation)state("polishing",DictationRules.modeLabel(job.mode)+" writing");});
                String instructions=DictationRules.instructions(rules,job.mode,job.defaultLanguage,job.language,job.spoken,job.literal,job.extra);
                DictationRules.Result value=api.generate(connection,store.secret(connection.getString("id")),instructions,job.wav,job.text,google,cancel);cancel.check();
                entry.put("transcript",value.transcript).put("text",value.text).put("language",value.language).put("uncertain",value.uncertain).put("status","success").put("error","");
                if(!keepAudio&&!audio.isEmpty()){store.audioFile(audio).delete();entry.put("audio","");}
                JSONObject completed=entry;main.post(()->{if(token!=generation)return;
                    boolean inserted=false;if(!cancel.cancelled()&&autoInsert&&job.destination!=null&&value.uncertain.length()==0){state("inserting","");try{inserted=job.destination.insert(value.text);}catch(Exception ignored){}}
                    try{completed.put("insertion",inserted?"inserted":"review");if(history||existingId!=null)store.putHistory(completed);}catch(Exception ignored){}
                    result=completed;String note=cancel.cancelled()?"Cancelled before insertion. Your finished text is ready to review.":inserted?"Inserted at your cursor.":value.uncertain.length()>0?"Review the unclear words, then insert your text.":job.destination!=null?"The original text field changed. Your words are saved in Sotto.":"Your words are ready.";state(cancel.cancelled()?"cancelled":"done",note);owner="";
                });
            }catch(Exception error){try{entry.put("status",cancel.cancelled()?"cancelled":"failed").put("error",safeError(error));if(history||existingId!=null)store.putHistory(entry);}catch(Exception ignored){}main.post(()->{if(token==generation){state(cancel.cancelled()?"cancelled":"error",cancel.cancelled()?"Dictation cancelled. Return to History to retry retained dictations.":safeError(error));owner="";}});}
        });
    }
    public <T> void background(Callable<T> work,Completion<T> callback){worker.execute(()->{try{T value=work.call();main.post(()->{callback.done(value);notifyChanged();});}catch(Exception error){main.post(()->callback.failed(safeError(error)));}});}
    public void connectGoogle(Completion<JSONObject> callback)throws Exception{if(busy())throw new Exception("Finish your current dictation first.");final long token=++generation;jobMode=store.mode();state("signing-in","Finish sign-in in your browser, then return to Sotto.");cancellation=new ApiClient.Cancellation();ApiClient.Cancellation cancel=cancellation;background(()->google.login(cancel,progress->main.post(()->{if(token==generation&&stage.equals("signing-in"))state("signing-in",progress);})),new Completion<JSONObject>(){public void done(JSONObject c){if(token!=generation)return;state("idle","Signed in. Choose a model to check account access.");callback.done(c);}public void failed(String error){if(token!=generation)return;state("error",error);callback.failed(error);}});}
    public static String safeError(Exception error){if(error instanceof CancellationException)return "Dictation cancelled. Your saved recording is available to retry.";if(error instanceof java.net.SocketTimeoutException)return "The provider took too long. Retry your saved recording.";if(error instanceof java.net.UnknownHostException)return "Could not reach the provider. Check your internet connection and retry.";if(error instanceof java.net.SocketException)return "The connection stopped. Check your network and retry.";if(error instanceof org.json.JSONException)return "The provider response was incomplete. Retry or choose a different model.";String message=error.getMessage();return message==null||message.length()>650?"Sotto could not finish. Check your connection and try again.":message.replaceAll("Bearer\\s+\\S+","Bearer [redacted]").replaceAll("AIza[\\w-]+|sk-[\\w-]{10,}","[redacted]");}
    private final class Job {
        byte[] wav;String text,language;int seconds;final Destination destination;final String mode,defaultLanguage,extra;final boolean spoken,literal;
        Job(byte[] wav,String text,int seconds,Destination destination){this.wav=wav;this.text=text;this.seconds=seconds;this.destination=destination;mode=store.mode();language=store.language();defaultLanguage=store.settings.getString("outputLanguage","English");extra=store.settings.getString("extraInstructions","");spoken=store.settings.getBoolean("spokenLanguage",true);literal=store.settings.getBoolean("literal",false);}
    }
}
