package dev.chux.gcp.crun.faults.socket.handlers;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

public class FaultHandlersModule extends AbstractModule {

  protected void configure() {
    final Multibinder<String> socketNames = Multibinder.newSetBinder(
      binder(), String.class, Names.named("socket-faults://names"));
    
    final MapBinder<String, SocketFaultHandler> handlersBinder = MapBinder.newMapBinder(
      binder(), String.class, SocketFaultHandler.class, Names.named("socket-faults://handlers"));


    handlersBinder.addBinding("immediate-termination")
      .to(ImmediateTermination.class).asEagerSingleton();
    socketNames.addBinding().toInstance("immediate-termination");

    handlersBinder.addBinding("reset-after-http-request-line")
      .to(ResetAfterHttpRequestLine.class).asEagerSingleton();
    socketNames.addBinding().toInstance("reset-after-http-request-line");
  }

}
