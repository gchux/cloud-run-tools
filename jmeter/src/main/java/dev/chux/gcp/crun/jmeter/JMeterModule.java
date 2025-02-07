package dev.chux.gcp.crun.jmeter;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.rest.Route;
import dev.chux.gcp.crun.jmeter.rest.RunJMeterTestController;
import dev.chux.gcp.crun.jmeter.config.JMeterTestProvider;

public class JMeterModule extends AbstractModule {

  protected void configure() {
    bind(String.class)
      .annotatedWith(Names.named("jmeter://test.jmx"))
      .toProvider(JMeterTestProvider.class);

    bind(String.class)
      .annotatedWith(Names.named("jmeter://jmx.dir"))
      .toProvider(JMeterTestProvider.class);

    install(new FactoryModuleBuilder()
      .implement(JMeterTest.class, JMeterTestImpl.class)
      .build(JMeterTestFactory.class));

    bind(JMeterTestService.class).in(Scopes.SINGLETON);

    final MapBinder<String, Route> routesBinder =
      MapBinder.newMapBinder(binder(), String.class, Route.class);

    routesBinder.addBinding("run-jmeter-test")
      .to(RunJMeterTestController.class).in(Scopes.SINGLETON);
  }

}
