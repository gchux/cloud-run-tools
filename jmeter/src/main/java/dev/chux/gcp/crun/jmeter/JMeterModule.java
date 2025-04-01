package dev.chux.gcp.crun.jmeter;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import com.google.common.collect.Maps;

import dev.chux.gcp.crun.jmeter.config.JMeterTestProvider;
import dev.chux.gcp.crun.jmeter.config.JMeterTestDirProvider;
import dev.chux.gcp.crun.jmeter.rest.RestModule;

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

    install(new RestModule());
  }

}
