package dev.chux.gcp.crun.faults.socket;

import static dev.chux.gcp.crun.App.of;

public class SocketFaultsApp {

  public static void main(final String[] args) {
    final SocketFaultsModule socketFaultsModule = new SocketFaultsModule();
    of(socketFaultsModule).run(socketFaultsModule, args);
  }

}
