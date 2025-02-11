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

public class ResetAfterHttpRequestLine extends AbstractSocketFaultHandler {
  private static final Logger logger = LoggerFactory.getLogger(ResetAfterHttpRequestLine.class);

  public static final String SOCKET_NAME = "reset-after-http-request-line";

  @Inject
  public ResetAfterHttpRequestLine(
    final ServerSocketsProvider serverSocketsProvider
  ) {
    super(SOCKET_NAME, serverSocketsProvider);
  }

  @Override
  protected void handle(final Socket socket) throws Exception {
    final BufferedReader in = super.newBufferedReader(socket);

    final String httpRequestLine = in.readLine();
    final SocketAddress address = super.getRemoteAddress(socket);

    if (isNullOrEmpty(httpRequestLine)) {
      logger.warn("missing HTTP request line: {}", address);
    } else {
      logger.info("got HTTP request line '{}' from: {}", httpRequestLine, address);
    }

    super.close(socket);
  }

}

