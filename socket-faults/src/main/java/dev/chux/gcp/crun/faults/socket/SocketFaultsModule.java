package dev.chux.gcp.crun.faults.socket;

import java.util.function.Consumer;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MapBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.AppMainThread;
import dev.chux.gcp.crun.annotations.MainThread;
import dev.chux.gcp.crun.faults.socket.handlers.FaultHandlersModule;
import dev.chux.gcp.crun.faults.socket.handlers.SocketFaultHandler;

public class SocketFaultsModule extends AbstractModule implements Consumer<Injector> {
  private static final Logger logger = LoggerFactory.getLogger(SocketFaultsModule.class);

  SocketFaultsModule() {}

  protected void configure() {
    bind(AppMainThread.class).annotatedWith(MainThread.class).to(SocketFaultsMainThread.class).asEagerSingleton();

    Multibinder.newSetBinder(binder(),
      String.class, Names.named("socket-faults://names"));

    MapBinder.newMapBinder(binder(),
      String.class, SocketFaultHandler.class, Names.named("socket-faults://handlers"));

    bind(ServerSocketsProvider.class)
      .to(ServerSocketsProviderImpl.class)
      .asEagerSingleton();

    install(new FaultHandlersModule());
  }

  @Override
  public void accept(final Injector injector) {
    logger.info("socket faults generator module started");
  }

}
