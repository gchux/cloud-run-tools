package dev.chux.gcp.crun.faults.bin;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import dev.chux.gcp.crun.faults.bin.Binary;
import dev.chux.gcp.crun.faults.bin.Curl;
import dev.chux.gcp.crun.model.HttpRequest;

public class CurlModule extends AbstractModule {

  protected void configure() {

    final TypeLiteral<String> keyType = new TypeLiteral<String>() {};
    final TypeLiteral<Binary<HttpRequest>> valueType = new TypeLiteral<Binary<HttpRequest>>() {};

    final MapBinder<String, Binary<HttpRequest>> curl = MapBinder.newMapBinder(
      binder(), keyType, valueType, Names.named("faults://binaries/curl"));

      curl.addBinding("faults://binaries/curl/linux").to(Curl.Linux.class).in(Scopes.SINGLETON);
      curl.addBinding("faults://binaries/curl/java").to(Curl.Java.class).in(Scopes.SINGLETON);
      curl.addBinding("faults://binaries/curl/python").to(Curl.Python.class).in(Scopes.SINGLETON);
      curl.addBinding("faults://binaries/curl/nodejs").to(Curl.NodeJS.class).in(Scopes.SINGLETON);
      curl.addBinding("faults://binaries/curl/golang").to(Curl.Golang.class).in(Scopes.SINGLETON);
  }

}
