package dev.chux.gcp.crun.jmeter.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;

import dev.chux.gcp.crun.rest.Route;

public class RestModule extends AbstractModule {

  static final String API_BASE = "test";

  protected void configure() {
    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);

    routesBinder.addBinding("jmeter://rest/run-test")
      .to(RunJMeterTestController.class).in(Scopes.SINGLETON);

    routesBinder.addBinding("jmeter://rest/stream-test")
      .to(StreamJMeterTestController.class).in(Scopes.SINGLETON);

    routesBinder.addBinding("jmeter://rest/get-test")
      .to(GetJMeterTestController.class).in(Scopes.SINGLETON);
  }

}
