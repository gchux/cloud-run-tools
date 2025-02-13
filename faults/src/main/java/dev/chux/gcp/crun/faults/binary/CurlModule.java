package dev.chux.gcp.crun.faults.binary;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

public class CurlModule extends AbstractModule {

  protected void configure() {
    final TypeLiteral curlBinaryType = new TypeLiteral<AbstractBinary<HttpRequest>>(){};

    install(new FactoryModuleBuilder()
      .implement(
        ManagedProcessProvider.class,
        Names.named("faults://binaries/curl/linux"),
        Curl.Linux.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named("faults://binaries/curl/java"),
        Curl.Java.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named("faults://binaries/curl/python"),
        Curl.Python.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named("faults://binaries/curl/nodejs"),
        Curl.NodeJS.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named("faults://binaries/curl/golang"),
        Curl.Golang.class
      )
      .build(CurlFactory.class));

    final TypeLiteral<String> keyType = new TypeLiteral<String>() {};
    final TypeLiteral<Binary<HttpRequest>> valueType = new TypeLiteral<Binary<HttpRequest>>() {};

    final MapBinder<String, Binary<HttpRequest>> curl = MapBinder.newMapBinder(
      binder(), keyType, valueType, Names.named("faults://binaries/curl"));

      // SINGLETON factories for curl managed process builder
      curl.addBinding("faults://binaries/curl/linux").to(Curl.Linux.class).in(Scopes.SINGLETON);
      curl.addBinding("faults://binaries/curl/java").to(Curl.Java.class).in(Scopes.SINGLETON);
      curl.addBinding("faults://binaries/curl/python").to(Curl.Python.class).in(Scopes.SINGLETON);
      curl.addBinding("faults://binaries/curl/nodejs").to(Curl.NodeJS.class).in(Scopes.SINGLETON);
      curl.addBinding("faults://binaries/curl/golang").to(Curl.Golang.class).in(Scopes.SINGLETON);
  }

}
