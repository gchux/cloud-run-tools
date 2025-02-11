package dev.chux.gcp.crun.jmeter;

import static dev.chux.gcp.crun.App.newApp;

public class JMeterApp {

  public static void main(final String[] args) {
    final JMeterModule jMeterModule = new JMeterModule();
    newApp(jMeterModule).run(args);
  }

}
