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


    handlersBinder.addBinding(ImmediateTermination.SOCKET_NAME)
      .to(ImmediateTermination.class).asEagerSingleton();
    socketNames.addBinding().toInstance(ImmediateTermination.SOCKET_NAME);

    handlersBinder.addBinding(ResetAfterHttpRequestLine.SOCKET_NAME)
      .to(ResetAfterHttpRequestLine.class).asEagerSingleton();
    socketNames.addBinding().toInstance(ResetAfterHttpRequestLine.SOCKET_NAME);

    handlersBinder.addBinding(ResetAfterHttpRequestHeaders.SOCKET_NAME)
      .to(ResetAfterHttpRequestHeaders.class).asEagerSingleton();
    socketNames.addBinding().toInstance(ResetAfterHttpRequestHeaders.SOCKET_NAME);

    handlersBinder.addBinding(ResetAfterHttpRequest.SOCKET_NAME)
      .to(ResetAfterHttpRequest.class).asEagerSingleton();
    socketNames.addBinding().toInstance(ResetAfterHttpRequest.SOCKET_NAME);

    handlersBinder.addBinding(ResetAfterHttpResponseLine.SOCKET_NAME)
      .to(ResetAfterHttpResponseLine.class).asEagerSingleton();
    socketNames.addBinding().toInstance(ResetAfterHttpResponseLine.SOCKET_NAME);

    handlersBinder.addBinding(ResetIncompleteHttpResponse.SOCKET_NAME)
      .to(ResetIncompleteHttpResponse.class).asEagerSingleton();
    socketNames.addBinding().toInstance(ResetIncompleteHttpResponse.SOCKET_NAME);

    handlersBinder.addBinding(ResetWithChoppedHttpResponseHeader.SOCKET_NAME)
      .to(ResetWithChoppedHttpResponseHeader.class).asEagerSingleton();
    socketNames.addBinding().toInstance(ResetWithChoppedHttpResponseHeader.SOCKET_NAME);

    handlersBinder.addBinding(ResetWithChoppedHttpResponseLine.SOCKET_NAME)
      .to(ResetWithChoppedHttpResponseLine.class).asEagerSingleton();
    socketNames.addBinding().toInstance(ResetWithChoppedHttpResponseLine.SOCKET_NAME);

    handlersBinder.addBinding(TimeoutBeforeHttpRequest.SOCKET_NAME)
      .to(TimeoutBeforeHttpRequest.class).asEagerSingleton();
    socketNames.addBinding().toInstance(TimeoutBeforeHttpRequest.SOCKET_NAME);

    handlersBinder.addBinding(TimeoutAfterHttpRequest.SOCKET_NAME)
      .to(TimeoutAfterHttpRequest.class).asEagerSingleton();
    socketNames.addBinding().toInstance(TimeoutAfterHttpRequest.SOCKET_NAME);

    handlersBinder.addBinding(TimeoutAfterHttpResponseHeaders.SOCKET_NAME)
      .to(TimeoutAfterHttpResponseHeaders.class).asEagerSingleton();
    socketNames.addBinding().toInstance(TimeoutAfterHttpResponseHeaders.SOCKET_NAME);
  }

}
