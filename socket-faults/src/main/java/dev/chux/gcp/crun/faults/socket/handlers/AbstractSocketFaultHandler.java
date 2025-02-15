package dev.chux.gcp.crun.faults.socket.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

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

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.ServerSocketsProvider;
import dev.chux.gcp.crun.faults.socket.handlers.SocketFaultHandler;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
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
    
    this.socketObservable.subscribe(this);
    this._disposable = this.socketObservable.connect();
    
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
    return new BufferedReader(new InputStreamReader(socket.getInputStream()), 1);
  }

  protected final BufferedWriter newBufferedWriter(final Socket socket) throws Exception {
    return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()), 1);
  }

  protected final SocketAddress getRemoteAddress(final Socket socket) throws Exception {
    return socket.getRemoteSocketAddress();
  }

  protected final void close(final Socket socket) throws Exception {
    logger.info("closing connection for handler '{}': {}", this.get(), getRemoteAddress(socket));
    socket.close();
  }

  protected final Optional<String> consumeHttpRequestLine(final Socket socket, final BufferedReader in) throws Exception {
    final String httpRequestLine = in.readLine();
    if (isNullOrEmpty(httpRequestLine)) {
      logger.warn("missing HTTP request line: {}", getRemoteAddress(socket));
      return absent();
    }
    logger.info("{} - got HTTP request line '{}' from: {}", this.get(), httpRequestLine, getRemoteAddress(socket));
    return fromNullable(emptyToNull(httpRequestLine));
  }

  protected final int consumeHttpRequestHeaders(final Socket socket, final BufferedReader in) throws Exception {
    String header;
    int contentLength = 0;
    while (!isNullOrEmpty(header = in.readLine())) {
      if (header.toLowerCase().startsWith("content-length:")) {
        contentLength = Integer.parseInt(header.split(":", 2)[1].trim(), 10);
      }
      logger.info("{} - got HTTP request header '{}' from: {}", this.get(), header, getRemoteAddress(socket));
    }
    logger.info("{} - got HTTP 'Content-Length: {}' from: {}", this.get(), contentLength, getRemoteAddress(socket));
    return contentLength;
  }

  protected final void consumeHttpRequestPayload(
    final Socket socket,
    final BufferedReader in,
    final int contentLength
  ) throws Exception {
    final StringBuilder data = new StringBuilder();
    int character;
    int index = 0;
    while((index < contentLength) && in.ready() && ((character = in.read()) != -1)) {
      data.append((char) character);
      index += 1;
    }
    logger.info("{} - got HTTP request payload '{}' from: {}", this.get(), data, getRemoteAddress(socket));
  }

  protected final void consumeHttpRequest(final Socket socket, final BufferedReader in) throws Exception {
    this.consumeHttpRequestLine(socket, in);
    final int contentLength = this.consumeHttpRequestHeaders(socket, in);
    this.consumeHttpRequestPayload(socket, in, contentLength);
  }

  protected final void write(
    final Socket socket,
    final BufferedWriter out,
    final String data
  ) throws Exception {
    out.write(data);
    logger.info("{} - wrote data '{}' to: {}", this.get(), data, getRemoteAddress(socket));
  }

  protected final void append(
    final Socket socket,
    final BufferedWriter out,
    final String data
  ) throws Exception {
    out.append(data);
    logger.info("{} - appended data '{}' to: {}", this.get(), data, getRemoteAddress(socket));
  }

  protected final void send(
    final Socket socket,
    final BufferedWriter out,
    final String data
  ) throws Exception {
    this.write(socket, out, data);
    out.flush();
    logger.info("{} - sent data '{}' to: {}", this.get(), data, getRemoteAddress(socket));
  }

  protected final void writeHttpResponseLine(
    final Socket socket,
    final BufferedWriter out,
    final int code,
    final String status
  ) throws Exception {
    out.append("HTTP/1.1 ")
      .append(Integer.toString(code, 10))
      .append(" ")
      .append(status);
    out.newLine();
    logger.info("{} - wrote HTTP response line 'HTTP/1.1 {} {}' to: {}", this.get(), code, status, getRemoteAddress(socket));
  }

  protected final void sendHttpResponseLine(
    final Socket socket,
    final BufferedWriter out,
    final int code,
    final String status
  ) throws Exception {
    this.writeHttpResponseLine(socket, out, code, status);
    out.flush();
    logger.info("{} - sent HTTP response line 'HTTP/1.1 {} {}' to: {}", this.get(), code, status, getRemoteAddress(socket));
  }

  protected final void writeHttpResponseHeader(
    final Socket socket,
    final BufferedWriter out,
    final String name,
    final String value
  ) throws Exception {
    out.append(name)
      .append(": ")
      .append(value);
    out.newLine();
    logger.info("{} - wrote HTTP response header '{}: {}' to: {}", this.get(), name, value, getRemoteAddress(socket));
  }

  protected final void sendHttpResponseHeader(
    final Socket socket,
    final BufferedWriter out,
    final String name,
    final String value
  ) throws Exception {
    this.writeHttpResponseHeader(socket, out, name, value);
    out.flush();
    logger.info("{} - sent HTTP response header '{}: {}' to: {}", this.get(), name, value, getRemoteAddress(socket));
  }

  protected final void writeHttpResponseBody(
    final Socket socket,
    final BufferedWriter out,
    final String data
  ) throws Exception {
    out.newLine();
    this.write(socket, out, data);
    logger.info("{} - wrote HTTP response body '{}' to: {}", this.get(), data, getRemoteAddress(socket));
  }

  protected final void sendHttpResponseBody(
    final Socket socket,
    final BufferedWriter out,
    final String data
  ) throws Exception {
    this.writeHttpResponseBody(socket, out, data);
    out.flush();
    logger.info("{} - sent HTTP response body '{}' to: {}", this.get(), data, getRemoteAddress(socket));
  }

}
