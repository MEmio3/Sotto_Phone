package studio.sotto.mobile;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class AudioCapture {
    public interface Callback { void complete(byte[] wav, int seconds); void failed(String message); void level(float value); }
    private volatile boolean running, cancelled; private AudioRecord recorder; private Thread worker;
    @SuppressWarnings("MissingPermission")
    public void start(Callback callback) throws Exception {
        if(running)throw new Exception("A recording is already running.");
        int size=Math.max(4096,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
        recorder=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size*2);
        if(recorder.getState()!=AudioRecord.STATE_INITIALIZED){recorder.release();recorder=null;throw new Exception("The microphone is unavailable. Check your device and try again.");}
        cancelled=false;running=true;try{recorder.startRecording();}catch(Exception error){running=false;recorder.release();recorder=null;throw new Exception("The microphone could not start. Check Android microphone access.");}
        worker=new Thread(()->{
            try(ByteArrayOutputStream pcm=new ByteArrayOutputStream()){
                byte[] buffer=new byte[size];int count;
                while(running&&pcm.size()<16000*2*180){count=recorder.read(buffer,0,buffer.length);if(count<0){if(!running)break;throw new Exception();}if(count==0)continue;pcm.write(buffer,0,count);long energy=0;for(int i=0;i+1<count;i+=2){short value=(short)((buffer[i]&255)|(buffer[i+1]<<8));energy+=(long)value*value;}callback.level((float)Math.min(1,Math.sqrt(energy/(double)Math.max(1,count/2))/8000));}
                if(!cancelled){if(pcm.size()<1600)callback.failed("The recording was too short. Speak for a moment and try again.");else callback.complete(wav(pcm.toByteArray()),pcm.size()/32000);}
            }catch(Exception error){if(!cancelled)callback.failed("The microphone stopped unexpectedly. Check your audio device and try again.");}
            finally{running=false;try{recorder.stop();}catch(Exception ignored){}recorder.release();recorder=null;}
        },"Sotto microphone");worker.start();
    }
    public void stop(){running=false;}
    public void cancel(){cancelled=true;running=false;}
    public static byte[] wav(byte[] pcm){ByteBuffer out=ByteBuffer.allocate(44+pcm.length).order(ByteOrder.LITTLE_ENDIAN);out.put(new byte[]{'R','I','F','F'}).putInt(36+pcm.length).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(pcm.length).put(pcm);return out.array();}
}
