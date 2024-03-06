package dev.chux.gcp.crun.jmeter;

public class JMeterTestConfig {

  private final String host;
  private final String path;
  private int concurrency = 1;
  private int duration = 1;
  private int rampupTime = 1;
  private int rampupSteps = 1;

  public JMeterTestConfig(final String host, final String path) {
    this.host = host;
    this.path = path;
  }

  public String host() {
    return this.host;
  }

  public String path() {
    return this.path;
  }

  public int concurrency() {
    return this.concurrency;
  }

  public JMeterTestConfig concurrency(final int concurrency) {
    this.concurrency = concurrency;
    return this;
  }

  public int duration() {
    return this.duration;
  }

  public JMeterTestConfig duration(final int duration) {
    this.duration = duration;
    return this;
  }

  public int rampupTime() {
    return this.rampupTime;
  }

  public JMeterTestConfig rampupTime(final int rampupTime) {
    this.rampupTime = rampupTime;
    return this;
  }

  public int rampupSteps() {
    return this.rampupSteps;
  }

  public JMeterTestConfig rampupSteps(final int rampupSteps) {
    this.rampupSteps = rampupSteps;
    return this;
  }

}
