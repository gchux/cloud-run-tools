package dev.chux.gcp.crun.faults.socket.handlers;

import java.net.Socket;
import java.net.ServerSocket;
import javax.annotation.PostConstruct;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.ServerSocketsProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.getStackTraceAsString;

abstract class AbstractSocketFaultHandler implements Supplier<String> {
  private static final Logger logger = LoggerFactory.getLogger(AbstractSocketFaultHandler.class);

  private final String socketName;
  private ServerSocket serverSocket;

  public AbstractSocketFaultHandler(
    final String socketName,
    final ServerSocketsProvider serverSocketsProvider
  ) {
    this.socketName = socketName;
    this.setServerSocket(serverSocketsProvider);
  }

  private final void setServerSocket(final ServerSocketsProvider serverSocketsProvider) {
    final Optional<ServerSocket> serverSocket = serverSocketsProvider.get(this.get());
    checkArgument(serverSocket.isPresent(), "socket not found: {}", socketName);
    this.serverSocket = serverSocket.get();
  }

  @Override
  public String get() {
    return this.socketName;
  }

  protected abstract void handle(final Socket socket) throws Exception;

  @PostConstruct()
  protected void init() {
    while (true) {
      this.accept();
    }
  }

  private void accept() {
    try {
      handle(serverSocket.accept());
    } catch(final Exception ex) {
      logger.error("error at socket handler '{}': {}", this.get(), getStackTraceAsString(ex));
    }
  }

}
