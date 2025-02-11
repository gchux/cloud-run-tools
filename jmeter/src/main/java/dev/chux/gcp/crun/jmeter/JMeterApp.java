package dev.chux.gcp.crun.jmeter;

import static dev.chux.gcp.crun.RestApp.of;

public class JMeterApp {

  public static void main(final String[] args) {
    final JMeterModule jMeterModule = new JMeterModule();
    of(jMeterModule).run(args);
  }

}
