package dev.chux.gcp.crun.faults.binary;

import com.google.inject.AbstractModule;

public class BinaryModule extends AbstractModule {

  protected void configure() {

    install(new CurlModule());

  }

}
