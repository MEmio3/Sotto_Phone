package studio.sotto.mobile;

/** Browser consent must return to the app before its network exchange begins. */
final class SignInForeground {
    private boolean visible;
    synchronized void visible(boolean value){visible=value;notifyAll();}
    synchronized boolean visible(){return visible;}
    synchronized void await(ApiClient.Cancellation cancel,long timeoutMillis)throws Exception{
        long deadline=System.nanoTime()+timeoutMillis*1000000L;
        while(!visible){cancel.check();long remaining=(deadline-System.nanoTime())/1000000L;if(remaining<=0)throw new Exception("Google returned the sign-in code, but Sotto was not reopened in time. Tap Connect Google again and use Return to Sotto.");wait(Math.min(100,Math.max(1,remaining)));}
        cancel.check();
    }
}
