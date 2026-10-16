package studio.sotto.mobile;

import org.junit.Test;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class SignInForegroundTest {
    @Test public void tokenExchangeWaitsForTheAppToReturnFromTheBrowser()throws Exception{
        SignInForeground foreground=new SignInForeground();CountDownLatch waiting=new CountDownLatch(1);ExecutorService worker=Executors.newSingleThreadExecutor();try{Future<Boolean> exchange=worker.submit(()->{waiting.countDown();foreground.await(new ApiClient.Cancellation(),3000);return true;});assertTrue(waiting.await(1,TimeUnit.SECONDS));try{exchange.get(100,TimeUnit.MILLISECONDS);fail("Must not exchange a code while app is backgrounded");}catch(TimeoutException expected){}foreground.visible(true);assertTrue(exchange.get(1,TimeUnit.SECONDS));}finally{worker.shutdownNow();}
    }
    @Test public void cancellationAndTimeoutDoNotStartTheExchange()throws Exception{
        SignInForeground foreground=new SignInForeground();ApiClient.Cancellation cancellation=new ApiClient.Cancellation();cancellation.cancel();try{foreground.await(cancellation,100);fail("Expected cancel");}catch(CancellationException expected){}try{foreground.await(new ApiClient.Cancellation(),30);fail("Expected bounded wait");}catch(Exception expected){assertTrue(expected.getMessage().contains("Return to Sotto"));}
    }
    @Test public void foregroundStateDoesNotSurviveAnotherBrowserSwitch()throws Exception{SignInForeground foreground=new SignInForeground();foreground.visible(true);foreground.await(new ApiClient.Cancellation(),50);foreground.visible(false);assertFalse(foreground.visible());try{foreground.await(new ApiClient.Cancellation(),30);fail("Paused app must wait again");}catch(Exception expected){assertTrue(expected.getMessage().contains("not reopened"));}}
}
