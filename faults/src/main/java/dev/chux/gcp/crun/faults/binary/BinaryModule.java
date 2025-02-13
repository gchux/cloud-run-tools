package dev.chux.gcp.crun.faults.binary;

import com.google.inject.AbstractModule;

public class BinaryModule extends AbstractModule {

  public static final String NAMESPACE = "faults://binaries";

  protected void configure() {

    install(new CurlModule());

  }

}
