package dev.chux.gcp.crun.faults.socket;

import java.util.function.Consumer;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MapBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.chux.gcp.crun.faults.socket.handlers.FaultHandlersModule;

import static com.google.common.base.Throwables.getStackTraceAsString;

public class SocketFaultsModule extends AbstractModule implements Consumer<Injector> {
  private static final Logger logger = LoggerFactory.getLogger(SocketFaultsModule.class);

  SocketFaultsModule() {}

  protected void configure() {
    bind(SocketFaultsMainThread.class).asEagerSingleton();

    final Multibinder<String> socketNames = Multibinder.newSetBinder(binder(), String.class, Names.named("socket-faults://names"));
    socketNames.addBinding().toInstance("immediate-termination");

    bind(ServerSocketsProvider.class).to(ServerSocketsProviderImpl.class).asEagerSingleton();

    install(new FaultHandlersModule());
  }

  @Override
  public void accept(final Injector injector) {
    try {
      logger.info("socket faults generator module started");
      final SocketFaultsMainThread mainThread = injector.getInstance(SocketFaultsMainThread.class);
      mainThread.await();
      logger.info("socket faults generator module terminated");
    } catch(final Exception ex) {
      logger.error("failed to start app: {}", getStackTraceAsString(ex));
    }
  }

}
