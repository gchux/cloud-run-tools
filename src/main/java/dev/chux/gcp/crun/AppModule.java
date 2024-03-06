package dev.chux.gcp.crun;

import com.google.inject.AbstractModule;
import dev.chux.gcp.crun.rest.RestModule;
import dev.chux.gcp.crun.process.ProcessModule;
import dev.chux.gcp.crun.jmeter.JMeterModule;

class AppModule extends AbstractModule {

  protected void configure() {
    install(new ProcessModule());
    install(new RestModule());
    install(new JMeterModule());
  }

}
