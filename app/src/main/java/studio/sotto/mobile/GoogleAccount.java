package studio.sotto.mobile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/** Community protocol parity with desktop Sotto, not an official Google SDK. */
public final class GoogleAccount {
    private static final String BOOTSTRAP="https://cloudcode-pa.googleapis.com/v1internal/", CONTENT="https://daily-cloudcode-pa.googleapis.com/v1internal/";
    private final Store store; private final Context context; private final ApiClient api; private final JSONObject client;
    private volatile ServerSocket loginServer;
    public GoogleAccount(Context context,Store store,ApiClient api)throws Exception{this.context=context.getApplicationContext();this.store=store;this.api=api;client=new JSONObject(new String(Store.bytes(context.getAssets().open("public-google-client.json"),10000),StandardCharsets.UTF_8));}
    public void cancelLogin(){ServerSocket server=loginServer;if(server!=null)try{server.close();}catch(Exception ignored){}}
    public JSONObject login(ApiClient.Cancellation cancel)throws Exception{
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);String state=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String redirect="http://127.0.0.1:20128/callback",code=null;
        try(ServerSocket server=new ServerSocket()){
            server.setReuseAddress(true);server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),20128));server.setSoTimeout(180000);loginServer=server;
            Map<String,String> params=new LinkedHashMap<>();params.put("client_id",client.getString("clientId"));params.put("redirect_uri",redirect);params.put("response_type","code");params.put("access_type","offline");params.put("prompt","consent");params.put("state",state);
            StringBuilder scopes=new StringBuilder();for(String scope:new String[]{"cloud-platform","userinfo.email","userinfo.profile","cclog","experimentsandconfigs"}){if(scopes.length()>0)scopes.append(' ');scopes.append("https://www.googleapis.com/auth/").append(scope);}params.put("scope",scopes.toString());
            String url="https://accounts.google.com/o/oauth2/v2/auth?"+ApiClient.form(params);
            new Handler(Looper.getMainLooper()).post(()->{try{context.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){cancelLogin();}});
            long deadline=System.currentTimeMillis()+180000;
            while(code==null&&System.currentTimeMillis()<deadline){cancel.check();server.setSoTimeout((int)Math.max(1000,deadline-System.currentTimeMillis()));
                try(Socket socket=server.accept()){
                    socket.setSoTimeout(5000);BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));String first=readLine(in,8192);String[] request=first.split(" ");Uri uri=Uri.parse("http://127.0.0.1"+(request.length>1?request[1]:"/"));
                    String received=uri.getQueryParameter("state");boolean valid="/callback".equals(uri.getPath())&&received!=null&&MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8),received.getBytes(StandardCharsets.UTF_8));
                    if(!valid){respond(socket,400,"Invalid sign-in state. Return to Sotto and try again.");continue;}
                    code=uri.getQueryParameter("code");if(uri.getQueryParameter("error")!=null||code==null){respond(socket,200,"Sign-in cancelled. Return to Sotto.");throw new Exception("Google sign-in was cancelled or denied.");}
                    respond(socket,200,"Sign-in received. You can return to Sotto while model access is checked.");
                }
            }
        }catch(SocketTimeoutException e){throw new Exception("Google sign-in timed out. Try connecting again.");}
        catch(BindException e){throw new Exception("The local sign-in port is busy. Close the app using it and try again.");}
        catch(SocketException e){throw new Exception("Google sign-in cancelled, or the browser could not be opened.");}
        finally{loginServer=null;}
        cancel.check();Map<String,String> form=new LinkedHashMap<>();form.put("grant_type","authorization_code");form.put("client_id",client.getString("clientId"));form.put("client_secret",client.getString("clientSecret"));form.put("code",code);form.put("redirect_uri",redirect);
        JSONObject tokens=api.request("https://oauth2.googleapis.com/token","POST",Collections.emptyMap(),ApiClient.form(form).getBytes(StandardCharsets.UTF_8),"application/x-www-form-urlencoded",cancel);
        if(tokens.optString("access_token").isEmpty())throw new Exception("Google did not return an access token.");
        JSONObject secret=new JSONObject().put("accessToken",tokens.getString("access_token")).put("refreshToken",tokens.optString("refresh_token")).put("expiresAt",System.currentTimeMillis()+tokens.optLong("expires_in",3600)*1000).put("projectId","");
        String email="Google account";try{email=api.json("https://www.googleapis.com/oauth2/v1/userinfo?alt=json","GET",ApiClient.key("compatible",secret.getString("accessToken")),null,cancel).optString("email",email);}catch(Exception ignored){}
        JSONObject connection=new JSONObject().put("id",UUID.randomUUID().toString()).put("name",email).put("type","antigravity").put("model","").put("experimental",true);store.saveConnection(connection,secret);return connection;
    }
    private static String readLine(BufferedReader reader,int limit)throws Exception{StringBuilder line=new StringBuilder();int value;while((value=reader.read())!=-1&&value!='\n'){if(line.length()>limit)throw new IOException("Invalid sign-in response.");if(value!='\r')line.append((char)value);}return line.toString();}
    private static void respond(Socket socket,int status,String message)throws Exception{byte[] body=("<!doctype html><html><meta name=\"viewport\" content=\"width=device-width\"><body style=\"background:#191919;color:#fff;font:18px system-ui;padding:32px\"><h1>Sotto</h1><p>"+message+"</p></body></html>").getBytes(StandardCharsets.UTF_8);String headers="HTTP/1.1 "+status+" OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'\r\nContent-Length: "+body.length+"\r\nConnection: close\r\n\r\n";socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().write(body);socket.getOutputStream().flush();}
    private void token(String id,JSONObject secret,ApiClient.Cancellation cancel)throws Exception{
        if(!secret.optString("accessToken").isEmpty()&&secret.optLong("expiresAt")>System.currentTimeMillis()+60000)return;
        if(secret.optString("refreshToken").isEmpty())throw new Exception("Reconnect this Google account in Connections.");
        Map<String,String> form=new LinkedHashMap<>();form.put("grant_type","refresh_token");form.put("client_id",client.getString("clientId"));form.put("client_secret",client.getString("clientSecret"));form.put("refresh_token",secret.getString("refreshToken"));
        JSONObject data=api.request("https://oauth2.googleapis.com/token","POST",Collections.emptyMap(),ApiClient.form(form).getBytes(StandardCharsets.UTF_8),"application/x-www-form-urlencoded",cancel);
        if(data.optString("access_token").isEmpty())throw new Exception("Google could not refresh this session. Reconnect your account.");secret.put("accessToken",data.getString("access_token")).put("expiresAt",System.currentTimeMillis()+data.optLong("expires_in",3600)*1000);
        if(!data.optString("refresh_token").isEmpty())secret.put("refreshToken",data.getString("refresh_token"));store.saveSecret(id,secret);
    }
    private JSONObject post(String action,JSONObject secret,JSONObject body,ApiClient.Cancellation cancel)throws Exception{
        boolean bootstrap=action.equals("loadCodeAssist")||action.equals("onboardUser");Map<String,String> headers=new HashMap<>();headers.put("Authorization","Bearer "+secret.getString("accessToken"));
        headers.put("User-Agent",bootstrap?"antigravity/2.5.5 darwin/arm64 google-api-nodejs-client/10.3.0":"antigravity/ide/2.5.5 darwin/arm64");if(bootstrap)headers.put("X-Goog-Api-Client","gl-node/22.21.1");
        try{return api.json((bootstrap?BOOTSTRAP:CONTENT)+action,"POST",headers,body,cancel);}catch(Exception e){if(e.getMessage()!=null&&e.getMessage().startsWith("The provider rejected"))throw new Exception("Google rejected "+(bootstrap?"account setup":action.equals("fetchAvailableModels")?"model discovery":"this dictation")+". Reconnect or choose a different available model.");throw e;}
    }
    private void discover(JSONObject secret,ApiClient.Cancellation cancel)throws Exception{
        String project=DictationRules.projectId(secret.opt("projectId"));secret.put("projectId",project);if(!project.isEmpty())return;
        JSONObject metadata=new JSONObject().put("ideType",9).put("platform",5).put("pluginType",2);
        JSONObject data=post("loadCodeAssist",secret,new JSONObject().put("metadata",metadata),cancel);project=DictationRules.extractProject(data);
        if(project.isEmpty()){
            JSONArray tiers=data.optJSONArray("allowedTiers");JSONObject chosen=null;if(tiers!=null)for(int i=0;i<tiers.length();i++){JSONObject tier=tiers.optJSONObject(i);if(tier!=null&&(chosen==null||tier.optBoolean("isDefault")))chosen=tier;}
            if(chosen==null||chosen.optString("id").isEmpty())throw new Exception("Signed in, but Google has not enabled model access. Complete Antigravity setup or use an API connection.");
            boolean pending=false;for(int attempt=0;attempt<3;attempt++){cancel.check();JSONObject onboard=post("onboardUser",secret,new JSONObject().put("tier_id",chosen.getString("id")).put("metadata",metadata),cancel);pending=onboard.has("done")&&!onboard.optBoolean("done");if(!pending){project=DictationRules.extractProject(onboard);break;}if(attempt<2)Thread.sleep(2000);}
            if(project.isEmpty())project=DictationRules.extractProject(post("loadCodeAssist",secret,new JSONObject().put("metadata",metadata),cancel));
            if(project.isEmpty()&&pending)throw new Exception("Google is still preparing this account. Wait a moment and choose Find models again.");
        }
        if(project.isEmpty())throw new Exception("Signed in, but Google has not returned a usable project. Complete Antigravity account setup, then find models again. An API connection works independently.");secret.put("projectId",project);
    }
    public JSONArray models(String id,JSONObject secret,ApiClient.Cancellation cancel)throws Exception{
        token(id,secret,cancel);try{discover(secret,cancel);}finally{store.saveSecret(id,secret);}
        JSONObject data=post("fetchAvailableModels",secret,new JSONObject().put("project",secret.getString("projectId")),cancel);Object raw=data.opt("models");if(raw==null)raw=data.opt("availableModels");JSONArray result=new JSONArray();
        if(raw instanceof JSONObject){JSONObject object=(JSONObject)raw;Iterator<String> keys=object.keys();while(keys.hasNext()){String key=keys.next();JSONObject item=object.optJSONObject(key);addModel(result,key,item==null?key:item.optString("displayName",key));}}
        else if(raw instanceof JSONArray){JSONArray list=(JSONArray)raw;for(int i=0;i<list.length();i++){JSONObject item=list.optJSONObject(i);if(item!=null)addModel(result,item.optString("id",item.optString("name")),item.optString("displayName",item.optString("name")));}}
        return result;
    }
    private static void addModel(JSONArray list,String id,String label)throws Exception{String lower=id.toLowerCase(Locale.ROOT);if(lower.contains("gemini")&&lower.contains("flash")&&!lower.matches(".*(image|live|tts).*"))list.put(new JSONObject().put("id",id).put("name",label.isEmpty()?id:label));}
    public JSONObject generate(JSONObject connection,JSONObject secret,JSONObject request,ApiClient.Cancellation cancel)throws Exception{
        String id=connection.getString("id");token(id,secret,cancel);try{discover(secret,cancel);}finally{store.saveSecret(id,secret);}
        return post("generateContent",secret,new JSONObject().put("project",secret.getString("projectId")).put("model",connection.getString("model")).put("requestId",UUID.randomUUID().toString()).put("userAgent","sotto").put("request",request),cancel);
    }
}
