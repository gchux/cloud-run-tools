package dev.chux.gcp.crun.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;

import com.google.common.reflect.TypeToken;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RestModule extends AbstractModule {

  protected void configure() {

    final Gson gson = new GsonBuilder()
      .excludeFieldsWithoutExposeAnnotation()
      .setVersion(1.0)
      .create();
    bind(Gson.class).toInstance(gson);

    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);
    routesBinder.addBinding("routes-info")
      .to(RestInfoController.class).in(Scopes.SINGLETON);

    bind(RestAPI.class).asEagerSingleton();
  }

}
