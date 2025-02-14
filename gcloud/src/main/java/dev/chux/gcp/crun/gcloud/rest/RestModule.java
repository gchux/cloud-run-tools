package dev.chux.gcp.crun.gcloud.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;

import dev.chux.gcp.crun.rest.Route;

public class RestModule extends AbstractModule {

  public static final String NAMESPACE = "gcloud://rest";

  protected void configure() {
    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);

    routesBinder.addBinding(RunGCloudCommandController.KEY)
      .to(RunGCloudCommandController.class).in(Scopes.SINGLETON);
  }

}
