package dev.chux.gcp.crun.jmeter;

import dev.chux.gcp.crun.App;

public class JMeterApp {

  public static void main(final String[] args) {
    final JMeterModule jMeterModule = new JMeterModule();
    final App app = new App(jMeterModule);
    app.run(args);
  }

}
