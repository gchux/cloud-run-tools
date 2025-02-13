package dev.chux.gcp.crun.faults.bin;

import com.google.inject.AbstractModule;

public class BinaryModule extends AbstractModule {

  protected void configure() {

    install(new CurlModule());

  }

}
