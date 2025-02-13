package dev.chux.gcp.crun.echo;

import static dev.chux.gcp.crun.RestApp.of;

public class EchoServerApp {

  public static void main(final String[] args) {
    final EchoServerModule module = new EchoServerModule();
    of(module).run(args);
  }

}
