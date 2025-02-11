package dev.chux.gcp.crun.faults.socket.handlers;

import com.google.inject.AbstractModule;

public class FaultHandlersModule extends AbstractModule {

  protected void configure() {

    bind(ImmediateTermination.class).asEagerSingleton();
  
  }

}
