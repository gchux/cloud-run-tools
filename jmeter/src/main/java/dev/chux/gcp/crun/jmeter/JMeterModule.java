package dev.chux.gcp.crun.jmeter;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import com.google.common.collect.Maps;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.rest.Route;
import dev.chux.gcp.crun.jmeter.config.JMeterTestProvider;
import dev.chux.gcp.crun.jmeter.config.JMeterTestDirProvider;
import dev.chux.gcp.crun.jmeter.rest.RunJMeterTestController;
import dev.chux.gcp.crun.jmeter.rest.StreamJMeterTestController;

public class JMeterModule extends AbstractModule {

  protected void configure() {
    bind(String.class)
      .annotatedWith(Names.named("jmeter://test.jmx"))
      .toProvider(JMeterTestProvider.class);

    bind(String.class)
      .annotatedWith(Names.named("jmeter://jmx.dir"))
      .toProvider(JMeterTestDirProvider.class);

    install(new FactoryModuleBuilder()
      .implement(JMeterTest.class, JMeterTestImpl.class)
      .build(JMeterTestFactory.class));

    final TypeLiteral<
      ConcurrentMap<String, JMeterTest>
    > concurrentStorageType = new TypeLiteral<
      ConcurrentMap<String, JMeterTest>
    >() {};
    final TypeLiteral<
      Map<String, JMeterTest>
    > storageType = new TypeLiteral<
      Map<String, JMeterTest>
    >() {};
    final ConcurrentMap<String, JMeterTest> storage = Maps.newConcurrentMap();
    bind(concurrentStorageType).toInstance(storage);
    bind(storageType).toInstance(storage);

    bind(RequestFileGenerator.class).in(Scopes.SINGLETON);
    bind(JMeterTestService.class).in(Scopes.SINGLETON);

    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);

    routesBinder.addBinding("jmeter://rest/run-test")
      .to(RunJMeterTestController.class).in(Scopes.SINGLETON);

    routesBinder.addBinding("jmeter://rest/stream-test")
      .to(StreamJMeterTestController.class).in(Scopes.SINGLETON);
  }

}
