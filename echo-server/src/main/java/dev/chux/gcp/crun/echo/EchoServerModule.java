package dev.chux.gcp.crun.echo;

import com.google.inject.AbstractModule;

import dev.chux.gcp.crun.echo.rest.RestModule;

public class EchoServerModule extends AbstractModule {

  protected void configure() {
    install(new RestModule());
  }

}
