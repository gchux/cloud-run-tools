package dev.chux.gcp.crun.faults.socket.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.net.Socket;
import java.net.SocketAddress;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.ServerSocketsProvider;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class ResetWithChoppedHttpResponseLine extends AbstractSocketFaultHandler {
  private static final Logger logger = LoggerFactory.getLogger(ResetWithChoppedHttpResponseLine.class);

  static final String SOCKET_NAME = "reset-with-chopped-http-response-line";

  @Inject
  public ResetWithChoppedHttpResponseLine(
    final ServerSocketsProvider serverSocketsProvider
  ) {
    super(SOCKET_NAME, serverSocketsProvider);
  }

  @Override
  protected void handle(final Socket socket) throws Exception {
    final BufferedReader in = super.newBufferedReader(socket);
    super.consumeHttpRequest(socket, in);

    final BufferedWriter out = super.newBufferedWriter(socket);
    super.send(socket, out, "HTTP/1.1 ");

    super.close(socket);
  }

}
