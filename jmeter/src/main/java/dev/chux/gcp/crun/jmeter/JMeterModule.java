package dev.chux.gcp.crun.jmeter;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;

import dev.chux.gcp.crun.rest.Route;

import dev.chux.gcp.crun.jmeter.rest.RunJMeterTestController;

public class JMeterModule extends AbstractModule {

  protected void configure() {
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
