package studio.sotto.mobile;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Debug-only, credential-free probes in the real app UID, without instrumentation priority. */
public final class NetworkProbeService extends Service {
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        String stage=intent==null?"":intent.getStringExtra("stage");
        if(!"foreground".equals(stage)&&!"background".equals(stage)&&!"handoff".equals(stage)){stopSelf(id);return START_NOT_STICKY;}
        boolean waitForReturn="handoff".equals(stage);
        new Thread(()->{
            File report=new File(getFilesDir(),"google-network-"+stage+".txt");
            try{
                Thread.sleep(6000);
                if(waitForReturn){Files.write(report.toPath(),"waiting-for-return".getBytes(StandardCharsets.UTF_8));((SottoApplication)getApplication()).signInForeground.await(new ApiClient.Cancellation(),45000);}
                Files.write(report.toPath(),GoogleNetworkDiagnostics.run(this).getBytes(StandardCharsets.UTF_8));
            }catch(Exception error){try{Files.write(report.toPath(),("Probe failed: "+error.getClass().getSimpleName()).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}
            finally{stopSelf(id);}
        },"Sotto debug network probe").start();
        return START_NOT_STICKY;
    }
}
