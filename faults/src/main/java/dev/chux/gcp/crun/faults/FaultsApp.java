package dev.chux.gcp.crun.faults;

import static dev.chux.gcp.crun.RestApp.of;

public class FaultsApp {

  public static void main(final String[] args) {
    final FaultsModule faultsModule = new FaultsModule();
    of(faultsModule).run(args);
  }

}
