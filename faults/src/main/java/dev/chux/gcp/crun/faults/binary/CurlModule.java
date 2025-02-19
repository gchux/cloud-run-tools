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
        Names.named(Curl.Linux.KEY),
        Curl.Linux.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.Java.KEY),
        Curl.Java.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.Python.KEY),
        Curl.Python.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.NodeJS.KEY),
        Curl.NodeJS.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.Golang.KEY),
        Curl.Golang.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.WithGoogleIdToken.KEY),
        Curl.WithGoogleIdToken.class
      )
      .implement(
        ManagedProcessProvider.class,
        Names.named(Curl.WithGoogleAuthToken.KEY),
        Curl.WithGoogleAuthToken.class
      );

    install(builder.build(CurlFactory.class));

    final TypeLiteral<String> keyType = new TypeLiteral<String>() {};
    final TypeLiteral<AbstractBinary<HttpRequest>> valueType = new TypeLiteral<AbstractBinary<HttpRequest>>() {};

    final MapBinder<String, AbstractBinary<HttpRequest>> curl = MapBinder
    .newMapBinder(binder(), keyType, valueType, Names.named(NAMESPACE));

    // SINGLETON factories for curl managed process builder
    curl.addBinding(Curl.Linux.KEY).to(Curl.Linux.class).in(Scopes.SINGLETON);
    curl.addBinding(Curl.Java.KEY).to(Curl.Java.class).in(Scopes.SINGLETON);
    curl.addBinding(Curl.Python.KEY).to(Curl.Python.class).in(Scopes.SINGLETON);
    curl.addBinding(Curl.NodeJS.KEY).to(Curl.NodeJS.class).in(Scopes.SINGLETON);
    curl.addBinding(Curl.Golang.KEY).to(Curl.Golang.class).in(Scopes.SINGLETON);
    curl.addBinding(Curl.WithGoogleIdToken.KEY).to(Curl.WithGoogleIdToken.class).in(Scopes.SINGLETON);
    curl.addBinding(Curl.WithGoogleAuthToken.KEY).to(Curl.WithGoogleAuthToken.class).in(Scopes.SINGLETON);
  }

}
