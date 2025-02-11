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

public class ResetAfterHttpRequestHeaders extends AbstractSocketFaultHandler {
  private static final Logger logger = LoggerFactory.getLogger(ResetAfterHttpRequestHeaders.class);

  static final String SOCKET_NAME = "reset-after-http-request-headers";

  @Inject
  public ResetAfterHttpRequestHeaders(
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
      super.close(socket);
      return;
    }

    logger.info("got HTTP request line '{}' from: {}", httpRequestLine, address);

    String header;
    while (!isNullOrEmpty(header = in.readLine())) {
      logger.info("got HTTP header '{}' from: {}", header, address);
    }
    super.close(socket);
  }

}


