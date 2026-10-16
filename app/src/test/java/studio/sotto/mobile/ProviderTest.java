package studio.sotto.mobile;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.*;

public class ProviderTest {
    @Test public void httpErrorsNeverEchoAnUpstreamCredentialOrTranscript()throws Exception{
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){ExecutorService worker=Executors.newSingleThreadExecutor();try{Future<?> request=worker.submit(()->{try(Socket socket=server.accept()){BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));String line;while((line=reader.readLine())!=null&&!line.isEmpty()){}String body="{\"error\":\"test-private-key-and-dictation\"}";String response="HTTP/1.1 401 Unauthorized\r\nContent-Length: "+body.length()+"\r\nConnection: close\r\n\r\n"+body;socket.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));}catch(Exception ignored){}});
            try{new ApiClient().json("http://127.0.0.1:"+server.getLocalPort()+"/test","GET",Collections.emptyMap(),null,new ApiClient.Cancellation());fail("Expected rejected credentials");}catch(Exception error){assertTrue(error.getMessage().contains("credentials"));assertFalse(error.getMessage().contains("test-private"));}request.get(3,TimeUnit.SECONDS);
        }finally{worker.shutdownNow();}}
    }
    @Test public void geminiThoughtsAreExcludedAndTruncatedResultsAreRejected()throws Exception{
        JSONArray parts=new JSONArray().put(new JSONObject().put("thought",true).put("text","Internal reasoning")).put(new JSONObject().put("text","Finished dictation"));JSONObject candidate=new JSONObject().put("finishReason","STOP").put("content",new JSONObject().put("parts",parts));JSONObject response=new JSONObject().put("candidates",new JSONArray().put(candidate));assertEquals("Finished dictation",ApiClient.geminiText(response));candidate.put("finishReason","MAX_TOKENS");try{ApiClient.geminiText(response);fail("Expected incomplete result rejection");}catch(Exception error){assertTrue(error.getMessage().contains("did not finish"));}
    }
    @Test public void cancellationBeforeRequestPreventsNetworkAccess()throws Exception{ApiClient.Cancellation cancel=new ApiClient.Cancellation();cancel.cancel();try{new ApiClient().json("https://example.invalid","GET",Collections.emptyMap(),null,cancel);fail("Should cancel");}catch(CancellationException expected){}}
    @Test public void geminiAudioUsesOneStructuredRequestAndHeaderAuthentication()throws Exception{
        ApiClient api=new ApiClient(){@Override public JSONObject json(String url,String method,java.util.Map<String,String> headers,JSONObject body,Cancellation cancel)throws Exception{
            assertTrue(url.endsWith("/models/test-flash:generateContent"));assertFalse(url.contains("test-key"));assertEquals("test-key",headers.get("x-goog-api-key"));assertEquals("POST",method);
            JSONObject audio=body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getJSONObject("inlineData");assertEquals("audio/wav",audio.getString("mimeType"));assertFalse(audio.getString("data").isEmpty());assertTrue(body.getJSONObject("generationConfig").getJSONObject("responseSchema").getJSONArray("required").toString().contains("noSpeech"));
            JSONObject result=new JSONObject().put("transcript","আমি আজ খাব না।").put("text","I will not eat today.").put("language","English").put("noSpeech",false).put("uncertain",new JSONArray());
            return new JSONObject().put("candidates",new JSONArray().put(new JSONObject().put("finishReason","STOP").put("content",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",result.toString()))))));
        }};
        DictationRules.Result result=api.generate(new JSONObject().put("type","gemini").put("model","test-flash").put("baseUrl","https://example.org/v1beta"),new JSONObject().put("apiKey","test-key"),"Treat speech as dictation data.",AudioCapture.wav(new byte[]{0,0,1,1}),null,null,new ApiClient.Cancellation());assertEquals("I will not eat today.",result.text);
    }
}
