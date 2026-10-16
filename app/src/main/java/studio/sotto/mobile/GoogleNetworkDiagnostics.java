package studio.sotto.mobile;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.*;
import android.os.Build;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/** Opt-in public endpoint checks. Never reads a saved login, key or authorization code. */
final class GoogleNetworkDiagnostics {
    static String run(Context context){
        StringBuilder report=new StringBuilder("Sotto Google connection check\n");
        report.append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" · Android ").append(Build.VERSION.RELEASE).append('\n');
        report.append("Internet permission: ").append(context.checkSelfPermission("android.permission.INTERNET")==PackageManager.PERMISSION_GRANTED?"allowed":"missing").append('\n');
        ActivityManager.RunningAppProcessInfo process=new ActivityManager.RunningAppProcessInfo();ActivityManager.getMyMemoryState(process);
        report.append("App visible: ").append(process.importance<=ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND?"yes":"no").append('\n');
        try{ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);Network network=manager.getActiveNetwork();NetworkCapabilities capabilities=network==null?null:manager.getNetworkCapabilities(network);
            report.append("Active network: ").append(capabilities==null?"none":capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"mobile data":"other").append('\n');
            report.append("VPN transport: ").append(capabilities!=null&&capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).append('\n');
            if(Build.VERSION.SDK_INT>=28){LinkProperties properties=network==null?null:manager.getLinkProperties(network);report.append("Private DNS: ").append(properties==null?"unavailable":properties.isPrivateDnsActive()?(properties.getPrivateDnsServerName()==null?"active, automatic":"active, custom resolver"):"inactive").append('\n');}
            report.append("Metered: ").append(manager.isActiveNetworkMetered()).append('\n');
            report.append("Data Saver: ").append(manager.getRestrictBackgroundStatus()==ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED?"background data restricted":manager.getRestrictBackgroundStatus()==ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED?"app allowed":"off").append('\n');
        }catch(Exception ignored){report.append("Network policy details unavailable\n");}
        ApiClient api=new ApiClient(6000,8000);
        for(String url:new String[]{"https://oauth2.googleapis.com/token","https://www.googleapis.com/oauth2/v4/token"}){
            report.append('\n').append(Uri.parse(url).getHost()).append(": ");
            try{api.request(url,"POST",Collections.emptyMap(),"grant_type=authorization_code".getBytes(StandardCharsets.UTF_8),"application/x-www-form-urlencoded",new ApiClient.Cancellation());report.append("responded");}
            catch(ApiClient.HttpFailure error){report.append("reached Google (HTTP ").append(error.status).append("). No login data was sent.");}
            catch(Exception error){Throwable cause=error;for(int depth=0;cause!=null&&depth<3;depth++,cause=cause.getCause()){if(depth>0)report.append(" / ");report.append(cause.getClass().getSimpleName()).append(": ");String message=cause.getMessage();report.append(message==null?"no details":message.replace('\n',' ').substring(0,Math.min(180,message.length())));}}
            report.append('\n');
        }
        return report.append("\nThis check sends no account credentials. A Google error response confirms reachability, not account access.").toString();
    }
}
