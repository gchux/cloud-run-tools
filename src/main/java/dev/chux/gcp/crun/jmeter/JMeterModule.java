package dev.chux.gcp.crun.jmeter;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;

public class JMeterModule extends AbstractModule {

  protected void configure() {
    install(new FactoryModuleBuilder()
        .implement(JMeterTest.class, JMeterTestImpl.class)
        .build(JMeterTestFactory.class));

    bind(JMeterTestService.class).in(Scopes.SINGLETON);
  }

}
