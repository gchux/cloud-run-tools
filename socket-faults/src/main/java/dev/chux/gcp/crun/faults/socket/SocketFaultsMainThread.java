package dev.chux.gcp.crun.faults.socket;

import java.util.concurrent.CountDownLatch;

import com.google.inject.Inject;

final class SocketFaultsMainThread extends Thread {
  
  private final CountDownLatch doneSignal;

  @Inject
  SocketFaultsMainThread() {
    this.doneSignal = new CountDownLatch(1);
    Runtime.getRuntime().addShutdownHook(this);
  }

  public void run() {
    this.doneSignal.countDown();
  }

  void await() throws InterruptedException {
    this.doneSignal.await();
  }
  
}
