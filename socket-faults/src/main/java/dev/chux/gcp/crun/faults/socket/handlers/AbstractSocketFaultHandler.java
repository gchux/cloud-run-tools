package dev.chux.gcp.crun.faults.socket.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.ServerSocket;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observables.ConnectableObservable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.ServerSocketsProvider;
import dev.chux.gcp.crun.faults.socket.handlers.SocketFaultHandler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getStackTraceAsString;

abstract class AbstractSocketFaultHandler implements SocketFaultHandler {

  private static final Logger logger = LoggerFactory.getLogger(AbstractSocketFaultHandler.class);

  private final String socketName;
  private final ServerSocket serverSocket;
  private final ConnectableObservable<Socket> socketObservable;
  private final AtomicBoolean isActive;
  private final AtomicReference<Disposable> disposable;
  private final CountDownLatch completeSignal;

  private Disposable _disposable;

  public AbstractSocketFaultHandler(
    final String socketName,
    final ServerSocketsProvider serverSocketsProvider
  ) {
    this.socketName = socketName;
    this.serverSocket = this.getServerSocket(serverSocketsProvider);
    this.socketObservable = this.newSocketObservable();
    this.isActive = new AtomicBoolean(false);
    this.disposable = new AtomicReference(null);
    this.completeSignal = new CountDownLatch(1);
  }

  private final ConnectableObservable<Socket> newSocketObservable() {
    return Observable.create(this)
      .observeOn(Schedulers.io())
      .subscribeOn(Schedulers.computation())
      .publish();
  }

  private final ServerSocket getServerSocket(final ServerSocketsProvider serverSocketsProvider) {
    final Optional<ServerSocket> serverSocket = serverSocketsProvider.get(this.get());
    checkArgument(serverSocket.isPresent(), "socket not found", socketName);
    return serverSocket.get();
  }

  @Override
  public Boolean start() {
    if (!this.isActive.compareAndSet(false, true)) {
      logger.warn("socket handler already started: {}", this.get());
      return Boolean.FALSE;
    }
    
    this._disposable = this.socketObservable.connect();
    this.socketObservable.subscribe(this);
    
    logger.info("socket handler started: {}", this.get());

    return Boolean.TRUE;
  }

  @Override
  public Boolean stop(final CountDownLatch stopSignal) {
    if (!this.isActive.compareAndSet(true, false)) {
      logger.warn("socket handler already stopped: {}", this.get());
      return Boolean.FALSE;
    }

    this.closeServerSocket();

    try {
      this.completeSignal.await(3L, TimeUnit.SECONDS);
    } catch(final Exception ex) {
      logger.error("'{}': {}", this.get(), getStackTraceAsString(ex));
    }

    checkNotNull(this._disposable).dispose();

    stopSignal.countDown();

    logger.info("socket handler stopped: {}", this.get());

    return Boolean.TRUE;
  }

  private void closeServerSocket() {
    try {
      this.serverSocket.close();
    } catch(final Exception ex) {
      logger.error("'{}': {}", this.get(), getStackTraceAsString(ex));
    }
  }

  @Override
  public Boolean isActive() {
    return Boolean.valueOf(this.isActive.get());
  }

  @Override
  public String get() {
    return this.socketName;
  }

  protected abstract void handle(final Socket socket) throws Exception;

  @Override
  public void subscribe(final ObservableEmitter<Socket> emitter) {
    while(this.isActive()) {
      try {
        emitter.onNext(this.serverSocket.accept());
      } catch(final Exception ex) {
        logger.error("'{}': {}", this.get(), getStackTraceAsString(ex));
      }
    }
    emitter.onComplete();
  }

  @Override
  public void onNext(final Socket socket) {
    try {
      logger.info("new connection for handler '{}': {}", this.get(), socket);
      this.handle(socket);
    } catch(final Exception ex) {
      logger.error("error handling connection '{}': {}", this.get(), getStackTraceAsString(ex));
    }
  }

  @Override
  public void onError(final Throwable error) {
    logger.error("error at socket handler '{}': {}", this.get(), getStackTraceAsString(error));
  }

  @Override
  public void onComplete() {
    checkNotNull(this.disposable.get()).dispose();
    logger.info("socket handler complete: {}", this.get());
    this.completeSignal.countDown(); 
  }

  @Override
  public void onSubscribe(final Disposable disposable) {
    checkState(this.disposable.compareAndSet(null, checkNotNull(disposable)), "disposable already set");
    checkState(!disposable.isDisposed());
    logger.info("new subscription for socket handler: {}", this.get());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .addValue(this.get())
      .add("isActive", this.isActive())
    .toString();
  }

  protected final BufferedReader newBufferedReader(final Socket socket) throws Exception {
    return new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  protected final void close(final Socket socket) throws Exception {
    logger.info("closing connection for handler '{}': {}", this.get(), socket.getRemoteSocketAddress());
    socket.close();
  }

  protected final SocketAddress getRemoteAddress(final Socket socket) throws Exception {
    return socket.getRemoteSocketAddress();
  }

}
