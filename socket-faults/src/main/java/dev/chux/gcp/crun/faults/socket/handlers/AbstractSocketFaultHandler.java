package dev.chux.gcp.crun.faults.socket.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.ServerSocket;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.Disposable;

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
  private final Observable<Socket> socketObservable;
  private final AtomicBoolean isActive;
  private final AtomicReference<Disposable> disposable;

  public AbstractSocketFaultHandler(
    final String socketName,
    final ServerSocketsProvider serverSocketsProvider
  ) {
    this.socketName = socketName;
    this.serverSocket = this.getServerSocket(serverSocketsProvider);
    this.socketObservable = this.newSocketObservable();
    this.isActive = new AtomicBoolean(true);
    this.disposable = new AtomicReference(null);
  }

  private final Observable<Socket> newSocketObservable() {
    return Observable.create(this).observeOn(Schedulers.io()).subscribeOn(Schedulers.computation());
  }

  private final ServerSocket getServerSocket(final ServerSocketsProvider serverSocketsProvider) {
    final Optional<ServerSocket> serverSocket = serverSocketsProvider.get(this.get());
    checkArgument(serverSocket.isPresent(), "socket not found", socketName);
    return serverSocket.get();
  }

  @Override
  public void start() {
    this.socketObservable.subscribe(this);
    logger.info("socket handler started: {}", this.get());
  }

  @Override
  public boolean stop() {
    final boolean stopped = this.isActive.compareAndSet(true, false);
    checkNotNull(this.disposable.get()).dispose();
    if (stopped) {
      logger.info("socket handler stopped: {}", this.get());
    }
    return stopped;
  }

  @Override
  public boolean isActive() {
    return this.isActive.get();
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
        emitter.onError(ex);
      }
    }
    logger.info("socket handler complete: {}", this.get());
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
  public void onComplete() {}

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
