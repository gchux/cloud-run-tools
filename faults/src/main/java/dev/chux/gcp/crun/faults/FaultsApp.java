package dev.chux.gcp.crun.faults;

import static dev.chux.gcp.crun.App.newApp;

public class FaultsApp {

  public static void main(final String[] args) {
    final FaultsModule faultsModule = new FaultsModule();
    newApp(faultsModule).run(args);
  }

}
