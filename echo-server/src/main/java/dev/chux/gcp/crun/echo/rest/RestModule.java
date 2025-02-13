package dev.chux.gcp.crun.echo.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;

import dev.chux.gcp.crun.rest.Route;

public class RestModule extends AbstractModule {

  public static final String NAMESPACE = "echo-server://rest";
  public static final String PROPERTIES_PREFIX = "echo-server.rest";

  protected void configure() {
    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);

    routesBinder.addBinding(EchoController.NAMESPACE)
      .to(EchoController.class).in(Scopes.SINGLETON);
  }

}
