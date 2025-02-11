package dev.chux.gcp.crun.faults.socket;

import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import java.util.concurrent.CountDownLatch;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.handlers.SocketFaultHandler;

import static com.google.common.base.Preconditions.checkNotNull;

final class SocketFaultsMainThread extends Thread {

  private static final Logger logger = LoggerFactory.getLogger(SocketFaultsMainThread.class);
  
  private final CountDownLatch doneSignal;
  private final Set<String> socketNames;
  private final Map<String, SocketFaultHandler> socketHandlers;

  @Inject
  SocketFaultsMainThread(
    @Named("socket-faults://names") final Set<String> socketNames,
    @Named("socket-faults://handlers") final Map<String, SocketFaultHandler> socketHandlers
  ) {
    this.doneSignal = new CountDownLatch(1);
    this.socketNames = socketNames;
    this.socketHandlers = socketHandlers;
    Runtime.getRuntime().addShutdownHook(this);
  }

  @PostConstruct
  void startSocketHandlers() {
    for(final String socketName : this.socketNames) {
      this.startSocketHandler(socketName);
    }
  }

  public void run() {
    for(final String socketName : this.socketNames) {
      this.stopSocketHandler(socketName);
    }
    this.doneSignal.countDown();
  }

  private void startSocketHandler(final String socketName) {
    final SocketFaultHandler handler = this.socketHandlers.get(socketName);
    logger.info("starting handler '{}': {}", socketName, handler);
    checkNotNull(handler, "handler not found:", socketName).start();

  }

  private void stopSocketHandler(final String socketName) {
    final SocketFaultHandler handler = this.socketHandlers.get(socketName);
    logger.info("stopping handler '{}': {}", socketName, handler);
    checkNotNull(handler, "handler not found:", socketName).stop();

  }

  void await() throws InterruptedException {
    this.doneSignal.await();
  }
  
}
