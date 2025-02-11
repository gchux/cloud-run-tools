package dev.chux.gcp.crun.jmeter;

import static dev.chux.gcp.crun.App.newRestApp;

public class JMeterApp {

  public static void main(final String[] args) {
    final JMeterModule jMeterModule = new JMeterModule();
    newRestApp(jMeterModule).runRestAPI(args);
  }

}
