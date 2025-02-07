package dev.chux.gcp.crun.gcloud;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.rest.Route;
import dev.chux.gcp.crun.gcloud.rest.RunGCloudCommandController;

public class GCloudModule extends AbstractModule {

  protected void configure() {
    bind(GCloudFormatSupplier.class).in(Scopes.SINGLETON);
    bind(String.class)
      .annotatedWith(Names.named("gcloud://format"))
      .toProvider(GCloudFormatSupplier.class);

    install(new FactoryModuleBuilder()
        .implement(GCloudCommand.class, GCloudCommandImpl.class)
        .build(GCloudCommandFactory.class));

    bind(GCloudService.class).in(Scopes.SINGLETON);

    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);

    routesBinder.addBinding("gcloud-command-runner")
      .to(RunGCloudCommandController.class).in(Scopes.SINGLETON);
  }

}
