package dev.chux.gcp.crun;

/**
 * Used to register a shutdown hook and waiting for termination.
 * The `run` method of the class implementing this interface
 * will be invoked when JVM is shutting down.
 * `await()` is called to allow the App time to complete its actions.
 */
public interface AppMainThread extends Runnable {
  
  public int await() throws InterruptedException;
    

}
