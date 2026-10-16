package studio.sotto.mobile;

import org.json.JSONObject;
import org.junit.Test;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;
import javax.net.ssl.SSLHandshakeException;
import static org.junit.Assert.*;

public class GoogleProtocolTest {
    private Map<String,String> grant(){Map<String,String> form=new LinkedHashMap<>();form.put("grant_type","authorization_code");form.put("client_id","synthetic-client");form.put("client_secret","synthetic-public-client-config");form.put("code","synthetic-private-code");form.put("redirect_uri","http://127.0.0.1:20128/callback");return form;}
    private JSONObject tokens()throws Exception{return new JSONObject().put("access_token","synthetic-access-token").put("refresh_token","synthetic-refresh-token").put("expires_in",3600);}
    @Test public void rpcUrlsMatchGoogleColonContractAndSeparateBootstrapFromInference(){
        assertEquals("https://cloudcode-pa.googleapis.com/v1internal:loadCodeAssist",GoogleProtocol.endpoint("loadCodeAssist",false));
        assertEquals("https://cloudcode-pa.googleapis.com/v1internal:onboardUser",GoogleProtocol.endpoint("onboardUser",false));
        assertEquals("https://daily-cloudcode-pa.googleapis.com/v1internal:fetchAvailableModels",GoogleProtocol.endpoint("fetchAvailableModels",false));
        assertEquals("https://daily-cloudcode-pa.googleapis.com/v1internal:generateContent",GoogleProtocol.endpoint("generateContent",false));
        assertEquals("https://cloudcode-pa.googleapis.com/v1internal:fetchAvailableModels",GoogleProtocol.endpoint("fetchAvailableModels",true));
        try{GoogleProtocol.endpoint("https://untrusted.example",false);fail("Unknown action");}catch(IllegalArgumentException expected){}
    }
    @Test public void dnsFailureAfterBrowserConsentUsesOnlyTheAlternateGoogleTokenHost()throws Exception{
        List<String> calls=new ArrayList<>();ApiClient api=new ApiClient(){@Override public JSONObject request(String url,String method,Map<String,String> headers,byte[] body,String type,Cancellation cancel)throws Exception{calls.add(url);assertEquals("POST",method);assertEquals("application/x-www-form-urlencoded",type);assertTrue(headers.isEmpty());assertFalse(url.contains("synthetic-private-code"));String form=new String(body,StandardCharsets.UTF_8);assertTrue(form.contains("code=synthetic-private-code"));assertTrue(form.contains("redirect_uri=http%3A%2F%2F127.0.0.1%3A20128%2Fcallback"));if(calls.size()==1)throw new UnknownHostException("DNS unavailable");return tokens();}};
        JSONObject result=new GoogleProtocol(api).tokens(grant(),new ApiClient.Cancellation());assertEquals("synthetic-access-token",result.getString("access_token"));assertEquals(Arrays.asList("https://oauth2.googleapis.com/token","https://www.googleapis.com/oauth2/v4/token"),calls);
    }
    @Test public void refreshUsesTheSameSafeFallbackWithoutChangingTheGrant()throws Exception{
        final int[] calls={0};Map<String,String> refresh=grant();refresh.remove("code");refresh.put("grant_type","refresh_token");refresh.put("refresh_token","synthetic-refresh-token");ApiClient api=new ApiClient(){@Override public JSONObject request(String url,String method,Map<String,String> headers,byte[] body,String type,Cancellation cancel)throws Exception{calls[0]++;String form=new String(body,StandardCharsets.UTF_8);assertTrue(form.contains("grant_type=refresh_token"));assertFalse(form.contains("code="));if(calls[0]==1)throw new ConnectException("Unavailable");return tokens();}};new GoogleProtocol(api).tokens(refresh,new ApiClient.Cancellation());assertEquals(2,calls[0]);
    }
    @Test public void twoDnsFailuresExposeTheFailedStageWithoutPrivateDetails()throws Exception{
        final int[] calls={0};ApiClient api=new ApiClient(){@Override public JSONObject request(String url,String method,Map<String,String> headers,byte[] body,String type,Cancellation cancel)throws Exception{calls[0]++;throw new UnknownHostException("synthetic-private-code");}};
        try{new GoogleProtocol(api).tokens(grant(),new ApiClient.Cancellation());fail("Expected DNS failure");}catch(Exception error){assertTrue(error.getMessage().contains("Google token exchange"));assertTrue(error.getMessage().contains("resolve"));assertTrue(error.getMessage().contains("Private DNS"));assertFalse(error.getMessage().contains("synthetic-private-code"));}assertEquals(2,calls[0]);
    }
    @Test public void rejectedAuthorizationCodeIsNeverReplayedAndDoesNotSuggestAnAudioProblem()throws Exception{
        final int[] calls={0};ApiClient api=new ApiClient(){@Override public JSONObject request(String url,String method,Map<String,String> headers,byte[] body,String type,Cancellation cancel)throws Exception{calls[0]++;throw new HttpFailure(400,"invalid_grant");}};
        try{new GoogleProtocol(api).tokens(grant(),new ApiClient.Cancellation());fail("Expected expired code");}catch(Exception error){assertTrue(error.getMessage().contains("expired or was already used"));assertFalse(error.getMessage().contains("audio"));}assertEquals(1,calls[0]);
    }
    @Test public void timeoutOrTlsFailureNeverReplaysAnAuthorizationCode()throws Exception{
        for(Exception failure:new Exception[]{new SocketTimeoutException("Read timeout"),new SSLHandshakeException("TLS rejected")}){final int[] calls={0};ApiClient api=new ApiClient(){@Override public JSONObject request(String url,String method,Map<String,String> headers,byte[] body,String type,Cancellation cancel)throws Exception{calls[0]++;throw failure;}};try{new GoogleProtocol(api).tokens(grant(),new ApiClient.Cancellation());fail("Expected failure");}catch(Exception error){assertTrue(error.getMessage().contains("Google token exchange"));}assertEquals(1,calls[0]);}
    }
    @Test public void clientRejectionIsDistinguishedFromANetworkFailure()throws Exception{
        ApiClient api=new ApiClient(){@Override public JSONObject request(String url,String method,Map<String,String> headers,byte[] body,String type,Cancellation cancel)throws Exception{throw new HttpFailure(401,"invalid_client");}};try{new GoogleProtocol(api).tokens(grant(),new ApiClient.Cancellation());fail("Expected client rejection");}catch(Exception error){assertTrue(error.getMessage().contains("OAuth client"));assertFalse(error.getMessage().contains("internet"));}
    }
    @Test public void bootstrapAndGenerationKeepTheirRequiredHeadersAndOriginalPayload()throws Exception{
        List<String> calls=new ArrayList<>();JSONObject request=new JSONObject().put("project","synthetic-project").put("model","synthetic-flash");ApiClient api=new ApiClient(){@Override public JSONObject json(String url,String method,Map<String,String> headers,JSONObject body,Cancellation cancel)throws Exception{calls.add(url);assertSame(request,body);assertEquals("Bearer synthetic-access-token",headers.get("Authorization"));assertEquals("POST",method);if(url.endsWith(":loadCodeAssist")){assertEquals("gl-node/22.21.1",headers.get("X-Goog-Api-Client"));assertTrue(headers.get("User-Agent").contains("google-api-nodejs-client"));}else assertTrue(headers.get("User-Agent").startsWith("antigravity/ide/"));return new JSONObject().put("ok",true);}};
        GoogleProtocol protocol=new GoogleProtocol(api);assertTrue(protocol.post("loadCodeAssist","synthetic-access-token",request,new ApiClient.Cancellation()).getBoolean("ok"));assertTrue(protocol.post("generateContent","synthetic-access-token",request,new ApiClient.Cancellation()).getBoolean("ok"));assertEquals(2,calls.size());
    }
    @Test public void discoveryCanRecoverWhenTheDailyHostnameCannotBeResolved()throws Exception{
        List<String> calls=new ArrayList<>();ApiClient api=new ApiClient(){@Override public JSONObject json(String url,String method,Map<String,String> headers,JSONObject body,Cancellation cancel)throws Exception{calls.add(url);if(calls.size()==1)throw new UnknownHostException("Daily endpoint unavailable");return new JSONObject().put("models",new JSONObject().put("synthetic-flash",new JSONObject()));}};
        assertTrue(new GoogleProtocol(api).post("fetchAvailableModels","synthetic-access-token",new JSONObject(),new ApiClient.Cancellation()).has("models"));assertEquals("https://cloudcode-pa.googleapis.com/v1internal:fetchAvailableModels",calls.get(1));
    }
    @Test public void quotaAndPermissionRejectionsAreNotRetriedOnOtherHosts()throws Exception{
        for(int status:new int[]{403,429}){final int[] calls={0};ApiClient api=new ApiClient(){@Override public JSONObject json(String url,String method,Map<String,String> headers,JSONObject body,Cancellation cancel)throws Exception{calls[0]++;throw new HttpFailure(status,"");}};try{new GoogleProtocol(api).post("fetchAvailableModels","synthetic-access-token",new JSONObject(),new ApiClient.Cancellation());fail("Expected rejection");}catch(Exception error){assertTrue(error.getMessage().contains("Google model discovery"));assertTrue(error.getMessage().contains("HTTP "+status));}assertEquals(1,calls[0]);}
    }
    @Test public void cancellationStopsBeforeAnotherTokenRequest()throws Exception{
        final int[] calls={0};ApiClient api=new ApiClient(){@Override public JSONObject request(String url,String method,Map<String,String> headers,byte[] body,String type,Cancellation cancel)throws Exception{calls[0]++;cancel.cancel();throw new UnknownHostException("Unavailable");}};try{new GoogleProtocol(api).tokens(grant(),new ApiClient.Cancellation());fail("Expected cancel");}catch(CancellationException expected){}assertEquals(1,calls[0]);
    }
}
