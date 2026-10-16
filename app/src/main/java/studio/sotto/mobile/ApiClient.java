package studio.sotto.mobile;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;

public class ApiClient {
    public static final class Cancellation {
        private volatile boolean cancelled; private volatile HttpURLConnection active;
        public void cancel(){cancelled=true;HttpURLConnection connection=active;if(connection!=null)connection.disconnect();}
        public void check(){if(cancelled||Thread.currentThread().isInterrupted())throw new CancellationException("Dictation cancelled.");}
        public boolean cancelled(){return cancelled;}
    }
    public JSONObject json(String url,String method,Map<String,String> headers,JSONObject body,Cancellation cancel) throws Exception {
        return request(url,method,headers,body==null?null:body.toString().getBytes(StandardCharsets.UTF_8),"application/json",cancel);
    }
    public JSONObject request(String url,String method,Map<String,String> headers,byte[] body,String type,Cancellation cancel) throws Exception {
        if(cancel!=null)cancel.check();HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(20000);connection.setReadTimeout(90000);connection.setRequestMethod(method);
        if(cancel!=null){cancel.active=connection;cancel.check();}
        try{
            for(Map.Entry<String,String> header:headers.entrySet())connection.setRequestProperty(header.getKey(),header.getValue());
            if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type",type);connection.setFixedLengthStreamingMode(body.length);try(OutputStream out=connection.getOutputStream()){out.write(body);}}
            int status=connection.getResponseCode();if(status<200||status>=300)throw new Exception(httpError(status));
            String text=new String(Store.bytes(connection.getInputStream(),2000000),StandardCharsets.UTF_8);
            if(cancel!=null)cancel.check();try{return new JSONObject(text);}catch(Exception e){throw new Exception("The provider returned an invalid response. Retry or choose a different model.");}
        }finally{connection.disconnect();if(cancel!=null)cancel.active=null;}
    }
    public static String httpError(int code){switch(code){case 400:return "The provider rejected the request. Check this model's audio support.";case 401:return "Your credentials expired or were rejected. Reconnect in Connections.";case 403:return "This account does not have access to that model or service.";case 404:return "That model or API endpoint was not found. Refresh the model list.";case 413:return "The recording is too large. Try a shorter recording.";case 429:return "Your provider's usage limit was reached. Wait or choose another connection.";default:return "The provider returned HTTP "+code+". Try again shortly.";}}
    public static Map<String,String> key(String type,String key){Map<String,String> headers=new HashMap<>();if(!key.isEmpty())headers.put(type.equals("gemini")?"x-goog-api-key":"Authorization",type.equals("gemini")?key:"Bearer "+key);return headers;}
    public JSONArray models(JSONObject connection,JSONObject secret,GoogleAccount google,Cancellation cancel)throws Exception{
        String type=connection.getString("type");if(type.equals("antigravity"))return google.models(connection.getString("id"),secret,cancel);
        JSONObject response=json(DictationRules.safeUrl(connection.getString("baseUrl"))+"/models","GET",key(type,secret.optString("apiKey")),null,cancel);JSONArray result=new JSONArray();
        JSONArray models=response.optJSONArray(type.equals("gemini")?"models":"data");if(models==null)return result;
        for(int i=0;i<models.length();i++){JSONObject model=models.optJSONObject(i);if(model==null)continue;String id=model.optString(type.equals("gemini")?"name":"id").replaceFirst("^models/","");
            if(type.equals("gemini")&&((model.optJSONArray("supportedGenerationMethods")==null||!model.optJSONArray("supportedGenerationMethods").toString().contains("generateContent"))||id.matches("(?i).*(image|tts|live|embedding).*")))continue;
            result.put(new JSONObject().put("id",id).put("name",model.optString("displayName",id)));}return result;
    }
    public DictationRules.Result generate(JSONObject connection,JSONObject secret,String instructions,byte[] wav,String text,GoogleAccount google,Cancellation cancel)throws Exception{
        String type=connection.getString("type"),base=connection.optString("baseUrl"),model=connection.getString("model");String transcript=text;
        if(type.equals("compatible")&&(transcript==null||transcript.isEmpty()))transcript=transcribe(connection,secret,wav,cancel);
        JSONObject data;
        if(type.equals("compatible")){
            JSONArray messages=new JSONArray().put(new JSONObject().put("role","system").put("content",instructions)).put(new JSONObject().put("role","user").put("content","DICTATION DATA:\n"+transcript));
            data=json(DictationRules.safeUrl(base)+"/chat/completions","POST",key(type,secret.optString("apiKey")),new JSONObject().put("model",model).put("stream",false).put("temperature",0.1).put("max_tokens",8192).put("messages",messages),cancel);
            JSONObject choice=data.getJSONArray("choices").getJSONObject(0);if(!"stop".equals(choice.optString("finish_reason","stop")))throw new Exception("The provider did not finish this dictation. Try a shorter recording.");
            DictationRules.Result result=DictationRules.parse(choice.getJSONObject("message").getString("content"));return new DictationRules.Result(transcript,result.text,result.language,result.uncertain);
        }
        JSONArray parts=new JSONArray();if(transcript!=null&&!transcript.isEmpty())parts.put(new JSONObject().put("text","DICTATION DATA:\n"+transcript));else parts.put(new JSONObject().put("inlineData",new JSONObject().put("mimeType","audio/wav").put("data",java.util.Base64.getEncoder().encodeToString(wav)))).put(new JSONObject().put("text","Transcribe and rewrite this recording according to the dictation rules."));
        JSONObject request=new JSONObject().put("systemInstruction",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",instructions)))).put("contents",new JSONArray().put(new JSONObject().put("role","user").put("parts",parts))).put("generationConfig",new JSONObject().put("temperature",0.1).put("maxOutputTokens",8192).put("responseMimeType","application/json"));
        if(!type.equals("antigravity"))request.getJSONObject("generationConfig").put("responseSchema",responseSchema());
        if(type.equals("antigravity"))data=google.generate(connection,secret,request,cancel);
        else data=json(DictationRules.safeUrl(base)+"/models/"+encode(model.replaceFirst("^models/",""))+":generateContent","POST",key(type,secret.optString("apiKey")),request,cancel);
        DictationRules.Result result=DictationRules.parse(geminiText(data));return transcript==null?result:new DictationRules.Result(transcript,result.text,result.language,result.uncertain);
    }
    private String transcribe(JSONObject c,JSONObject secret,byte[] wav,Cancellation cancel)throws Exception{
        if(wav==null)throw new Exception("No recording is available. Record again.");String boundary="Sotto"+UUID.randomUUID().toString().replace("-","");ByteArrayOutputStream out=new ByteArrayOutputStream();
        String prefix="--"+boundary+"\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\n"+c.optString("transcriptionModel","whisper-1")+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"response_format\"\r\n\r\njson\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"dictation.wav\"\r\nContent-Type: audio/wav\r\n\r\n";
        out.write(prefix.getBytes(StandardCharsets.UTF_8));out.write(wav);out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        JSONObject response=request(DictationRules.safeUrl(c.getString("baseUrl"))+"/audio/transcriptions","POST",key("compatible",secret.optString("apiKey")),out.toByteArray(),"multipart/form-data; boundary="+boundary,cancel);String text=response.optString("text","").trim();if(text.isEmpty())throw new Exception("No speech was returned. Check the transcription model and microphone.");return text;
    }
    public static String geminiText(JSONObject data)throws Exception{
        JSONObject result=data.optJSONObject("response");if(result==null)result=data;
        JSONObject feedback=result.optJSONObject("promptFeedback");if(feedback!=null&&feedback.has("blockReason"))throw new Exception("The provider declined this recording. It is available to retry.");
        JSONArray candidates=result.optJSONArray("candidates");if(candidates==null||candidates.length()==0)throw new Exception("The provider returned no dictation. Retry or choose another model.");
        JSONObject candidate=candidates.getJSONObject(0);String finish=candidate.optString("finishReason","STOP");if(!finish.equals("STOP")&&!finish.equals("FINISH_REASON_UNSPECIFIED"))throw new Exception("The provider did not finish the dictation. Try a shorter recording.");
        JSONArray parts=candidate.getJSONObject("content").getJSONArray("parts");StringBuilder text=new StringBuilder();for(int i=0;i<parts.length();i++){JSONObject part=parts.getJSONObject(i);if(!part.optBoolean("thought",false))text.append(part.optString("text",""));}return text.toString();
    }
    public static String encode(String text)throws Exception{return URLEncoder.encode(text,"UTF-8").replace("+","%20");}
    public static JSONObject responseSchema()throws Exception{return new JSONObject().put("type","OBJECT").put("properties",new JSONObject().put("transcript",new JSONObject().put("type","STRING")).put("text",new JSONObject().put("type","STRING")).put("language",new JSONObject().put("type","STRING")).put("noSpeech",new JSONObject().put("type","BOOLEAN")).put("uncertain",new JSONObject().put("type","ARRAY").put("items",new JSONObject().put("type","STRING")))).put("required",new JSONArray().put("transcript").put("text").put("language").put("noSpeech").put("uncertain"));}
    public static String form(Map<String,String> values)throws Exception{StringBuilder result=new StringBuilder();for(Map.Entry<String,String> entry:values.entrySet()){if(result.length()>0)result.append('&');result.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));}return result.toString();}
}
