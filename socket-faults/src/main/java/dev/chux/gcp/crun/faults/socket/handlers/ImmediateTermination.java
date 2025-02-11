package dev.chux.gcp.crun.faults.socket.handlers;

import java.net.Socket;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.ServerSocketsProvider;

public class ImmediateTermination extends AbstractSocketFaultHandler {
  private static final Logger logger = LoggerFactory.getLogger(ImmediateTermination.class);

  static final String SOCKET_NAME = "immediate-termination";

  @Inject
  public ImmediateTermination(
    final ServerSocketsProvider serverSocketsProvider
  ) {
    super(SOCKET_NAME, serverSocketsProvider);
  }

  @Override
  protected void handle(final Socket socket) throws Exception {
    super.close(socket);
  }

}

