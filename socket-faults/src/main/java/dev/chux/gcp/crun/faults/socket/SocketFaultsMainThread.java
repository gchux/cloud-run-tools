package dev.chux.gcp.crun.faults.socket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import java.util.concurrent.CountDownLatch;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.handlers.SocketFaultHandler;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;

final class SocketFaultsMainThread extends Thread {

  private static final Logger logger = LoggerFactory.getLogger(SocketFaultsMainThread.class);
  
  private final CountDownLatch doneSignal;
  private final Set<String> socketNames;
  private final Map<String, SocketFaultHandler> socketHandlers;

  private int startedSocketHandlers;

  @Inject
  SocketFaultsMainThread(
    @Named("socket-faults://names") final Set<String> socketNames,
    @Named("socket-faults://handlers") final Map<String, SocketFaultHandler> socketHandlers
  ) {
    checkState(socketNames.size() == socketHandlers.size());
    this.doneSignal = new CountDownLatch(1);
    this.socketNames = socketNames;
    this.socketHandlers = socketHandlers;
    Runtime.getRuntime().addShutdownHook(this);
  }

  @PostConstruct
  void startSocketHandlers() {
    for(final String socketName : this.socketNames) {
      if (this.startSocketHandler(socketName)) {
        this.startedSocketHandlers += 1;
      }
    }
    logger.info("started {} socket handlers", this.startedSocketHandlers);
  }

  public void run() {
    final CountDownLatch stopSignal = newStopSignal();
    for(final String socketName : this.socketNames) {
      this.stopSocketHandler(socketName, stopSignal);
    }
    try {
      stopSignal.await(5L, TimeUnit.SECONDS);
    } catch(final Exception ex) {
      logger.error("{}", getStackTraceAsString(ex));
    }
    this.doneSignal.countDown();
  }

  private CountDownLatch newStopSignal() {
    return new CountDownLatch(this.startedSocketHandlers);
  }

  private Boolean startSocketHandler(final String socketName) {
    final SocketFaultHandler handler = this.socketHandlers.get(socketName);
    logger.info("starting socket handler '{}': {}", socketName, handler);
    return checkNotNull(handler, "handler not found:", socketName).start();
  }

  private Boolean stopSocketHandler(final String socketName, final CountDownLatch stopSignal) {
    final SocketFaultHandler handler = this.socketHandlers.get(socketName);
    logger.info("stopping socket handler '{}': {}", socketName, handler);
    return checkNotNull(handler, "handler not found:", socketName).stop(stopSignal);
  }

  void await() throws InterruptedException {
    this.doneSignal.await();
  }
  
}
