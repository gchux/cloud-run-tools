package dev.chux.gcp.crun.faults.socket.handlers;

import java.io.BufferedReader;
import java.net.Socket;
import java.net.SocketAddress;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.ServerSocketsProvider;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class TimeoutBeforeHttpRequest extends AbstractSocketFaultHandler {
  private static final Logger logger = LoggerFactory.getLogger(TimeoutBeforeHttpRequest.class);

  static final String SOCKET_NAME = "timeout-before-http-request";

  @Inject
  public TimeoutBeforeHttpRequest(
    final ServerSocketsProvider serverSocketsProvider
  ) {
    super(SOCKET_NAME, serverSocketsProvider);
  }

  @Override
  protected void handle(final Socket socket) throws Exception {
    final BufferedReader in = super.newBufferedReader(socket);
    super.pauseSeconds(socket, 300);
    // HTTP client should timeout before 5m
    super.consumeHttpRequest(socket, in);
    super.close(socket);
  }

}
