package dev.chux.gcp.crun.faults;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;

import dev.chux.gcp.crun.rest.Route;

import dev.chux.gcp.crun.faults.binary.BinaryModule;
import dev.chux.gcp.crun.faults.command.FaultCommand;
import dev.chux.gcp.crun.faults.command.FaultCommandFactory;
import dev.chux.gcp.crun.faults.command.HttpRequestCommand;

import dev.chux.gcp.crun.faults.rest.RunHttpFaultController;

public class FaultsModule extends AbstractModule {

  protected void configure() {
    install(new FactoryModuleBuilder()
      .implement(
        FaultCommand.class,
        Names.named("faults://commands/httpRequest"),
        HttpRequestCommand.class
      )
      .build(FaultCommandFactory.class));

    bind(FaultsService.class).in(Scopes.SINGLETON);

    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);

    routesBinder.addBinding("http-fault-generator")
      .to(RunHttpFaultController.class).in(Scopes.SINGLETON);

    install(new BinaryModule());
  }

}
