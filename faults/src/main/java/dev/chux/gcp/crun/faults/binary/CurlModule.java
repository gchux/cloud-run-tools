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

  public static final String NAMESPACE = BinaryModule.NAMESPACE + "/curl";

  protected void configure() {
    final FactoryModuleBuilder builder = new FactoryModuleBuilder();

    builder
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.Linux.NAMESPACE),
        Curl.Linux.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.Java.NAMESPACE),
        Curl.Java.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.Python.NAMESPACE),
        Curl.Python.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.NodeJS.NAMESPACE),
        Curl.NodeJS.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.Golang.NAMESPACE),
        Curl.Golang.class
      );

    install(builder.build(CurlFactory.class));

    final TypeLiteral<String> keyType = new TypeLiteral<String>() {};
    final TypeLiteral<AbstractBinary<HttpRequest>> valueType = new TypeLiteral<AbstractBinary<HttpRequest>>() {};

    final MapBinder<String, AbstractBinary<HttpRequest>> curl = MapBinder
      .newMapBinder(binder(), keyType, valueType, Names.named(NAMESPACE));

      // SINGLETON factories for curl managed process builder
      curl.addBinding(Curl.Linux.NAMESPACE).to(Curl.Linux.class).in(Scopes.SINGLETON);
      curl.addBinding(Curl.Java.NAMESPACE).to(Curl.Java.class).in(Scopes.SINGLETON);
      curl.addBinding(Curl.Python.NAMESPACE).to(Curl.Python.class).in(Scopes.SINGLETON);
      curl.addBinding(Curl.NodeJS.NAMESPACE).to(Curl.NodeJS.class).in(Scopes.SINGLETON);
      curl.addBinding(Curl.Golang.NAMESPACE).to(Curl.Golang.class).in(Scopes.SINGLETON);
  }

}
