package studio.sotto.mobile;

import org.json.JSONObject;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;
import javax.net.ssl.SSLException;

/** Google-only endpoints, shared by sign-in, refresh, model discovery and dictation. */
final class GoogleProtocol {
    private static final String TOKEN="https://oauth2.googleapis.com/token";
    // Google's legacy token endpoint remains available on a separately resolved Google hostname.
    private static final String TOKEN_ALTERNATE="https://www.googleapis.com/oauth2/v4/token";
    private static final String BOOTSTRAP="https://cloudcode-pa.googleapis.com";
    private static final String CONTENT="https://daily-cloudcode-pa.googleapis.com";
    private final ApiClient api;
    GoogleProtocol(ApiClient api){this.api=api;}

    JSONObject tokens(Map<String,String> form,ApiClient.Cancellation cancel)throws Exception{
        boolean refreshing="refresh_token".equals(form.get("grant_type"));
        String step=refreshing?"Google session refresh":"Google token exchange";
        byte[] body=ApiClient.form(form).getBytes(StandardCharsets.UTF_8);
        String[] urls={TOKEN,TOKEN_ALTERNATE};
        for(int attempt=0;attempt<urls.length;attempt++){
            if(cancel!=null)cancel.check();
            try{return api.request(urls[attempt],"POST",Collections.emptyMap(),body,"application/x-www-form-urlencoded",cancel);}
            catch(Exception error){
                if(cancel!=null)cancel.check();
                // Retry only failures known to occur before a request reaches Google.
                // A read timeout, HTTP rejection or TLS failure must never replay an authorization code.
                if(beforeConnection(error)&&attempt+1<urls.length)continue;
                throw failure(step,error,true,refreshing);
            }
        }
        throw new IllegalStateException("Google token exchange did not run.");
    }

    static String endpoint(String action,boolean alternate){
        if(!Arrays.asList("loadCodeAssist","onboardUser","fetchAvailableModels","generateContent").contains(action))throw new IllegalArgumentException("Unsupported Google request.");
        boolean bootstrap=action.equals("loadCodeAssist")||action.equals("onboardUser");
        return (bootstrap||alternate?BOOTSTRAP:CONTENT)+"/v1internal:"+action;
    }
    static Map<String,String> headers(String action,String accessToken){
        boolean bootstrap=action.equals("loadCodeAssist")||action.equals("onboardUser");
        Map<String,String> headers=new HashMap<>();headers.put("Authorization","Bearer "+accessToken);
        headers.put("User-Agent",bootstrap?"antigravity/2.5.5 darwin/arm64 google-api-nodejs-client/10.3.0":"antigravity/ide/2.5.5 darwin/arm64");
        if(bootstrap)headers.put("X-Goog-Api-Client","gl-node/22.21.1");return headers;
    }
    JSONObject post(String action,String accessToken,JSONObject body,ApiClient.Cancellation cancel)throws Exception{
        String step=action.equals("loadCodeAssist")?"Google account check":action.equals("onboardUser")?"Google account setup":action.equals("fetchAvailableModels")?"Google model discovery":"Google dictation";
        boolean readOnly=action.equals("fetchAvailableModels")||action.equals("loadCodeAssist");
        for(int attempt=0;attempt<2;attempt++){
            if(cancel!=null)cancel.check();
            try{return api.json(endpoint(action,attempt>0),"POST",headers(action,accessToken),body,cancel);}
            catch(Exception error){
                if(cancel!=null)cancel.check();
                boolean missingReadOnly=readOnly&&error instanceof ApiClient.HttpFailure&&((ApiClient.HttpFailure)error).status==404;
                if(attempt==0&&(beforeConnection(error)||missingReadOnly))continue;
                throw failure(step,error,false,false);
            }
        }
        throw new IllegalStateException("Google request did not run.");
    }
    private static boolean beforeConnection(Exception error){return error instanceof UnknownHostException||error instanceof ConnectException||error instanceof NoRouteToHostException;}
    private static Exception failure(String step,Exception error,boolean token,boolean refresh){
        if(error instanceof CancellationException)return error;
        String retry=token?(refresh?"Reconnect this account if it continues.":"Tap Connect Google again after changing networks."):"Try Find models again, or use an API connection.";
        if(error instanceof UnknownHostException)return new Exception(step+" could not resolve Google's servers. Use Check Google connection to inspect app network access, Data Saver and Private DNS. "+retry,error);
        if(error instanceof ConnectException||error instanceof NoRouteToHostException)return new Exception(step+" could not connect to Google's servers. Check this app's network access or try another network. "+retry,error);
        if(error instanceof SocketTimeoutException)return new Exception(step+" timed out. "+retry,error);
        if(error instanceof SSLException)return new Exception(step+" could not establish a secure connection to Google. Check the phone's date/time and any VPN or HTTPS filtering. "+retry,error);
        if(error instanceof ApiClient.HttpFailure){ApiClient.HttpFailure http=(ApiClient.HttpFailure)error;
            if(token&&http.oauthError.equals("invalid_grant"))return new Exception(refresh?"Your Google session was revoked or expired. Reconnect this account.":"Google rejected the sign-in code because it expired or was already used. Tap Connect Google again.",error);
            if(token&&(http.oauthError.equals("invalid_client")||http.oauthError.equals("unauthorized_client")))return new Exception("Google rejected this experimental app's OAuth client. Use an API connection; another sign-in cannot fix client rejection.",error);
            if(token&&http.oauthError.equals("redirect_uri_mismatch"))return new Exception("Google rejected the sign-in callback address. This experimental login needs a compatibility update. Use an API connection.",error);
            String reason=http.status==403?"This account is not permitted to use the requested service.":http.status==401?"Reconnect this account.":http.status==404?"The Google service endpoint was not found.":http.status==429?"The account's usage limit was reached; wait before trying again.":"Retry or use an API connection.";
            return new Exception(step+" failed (HTTP "+http.status+"). "+reason,error);
        }
        return new Exception(step+" could not finish. Retry or use an API connection.",error);
    }
}
