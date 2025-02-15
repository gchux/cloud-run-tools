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

public class ResetIncompleteHttpResponse extends AbstractSocketFaultHandler {
  private static final Logger logger = LoggerFactory.getLogger(ResetIncompleteHttpResponse.class);

  static final String SOCKET_NAME = "reset-incomplete-http-response";

  @Inject
  public ResetIncompleteHttpResponse(
    final ServerSocketsProvider serverSocketsProvider
  ) {
    super(SOCKET_NAME, serverSocketsProvider);
  }

  @Override
  protected void handle(final Socket socket) throws Exception {
    final BufferedReader in = super.newBufferedReader(socket);
    super.consumeHttpRequest(socket, in);

    final BufferedWriter out = super.newBufferedWriter(socket);
    super.writeHttpResponseLine(socket, out, 200, "OK"); 
    super.sendHttpResponseHeader(socket, out, "Content-Length", "1000"); 
    super.sendHttpResponseBody(socket, out, "incomplete data");

    super.close(socket);
  }

}
