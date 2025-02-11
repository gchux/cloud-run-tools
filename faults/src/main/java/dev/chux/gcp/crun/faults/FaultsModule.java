package dev.chux.gcp.crun.faults;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.rest.Route;

import dev.chux.gcp.crun.faults.rest.RunHttpFaultController;

public class FaultsModule extends AbstractModule {

  protected void configure() {
    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);

    routesBinder.addBinding("http-fault-generator")
      .to(RunHttpFaultController.class).in(Scopes.SINGLETON);
  }

}
