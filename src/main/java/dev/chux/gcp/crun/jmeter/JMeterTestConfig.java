package dev.chux.gcp.crun.jmeter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class JMeterTestConfig {

  private final Optional<String> jmx;
  private final String host;
  private final String path;
  private int concurrency = 1;
  private int duration = 1;
  private int rampupTime = 1;
  private int rampupSteps = 1;
  
  public JMeterTestConfig(final String host, final String path) {
    this(null, host, path);
  }

  public JMeterTestConfig(final String jmx, final String host, final String path) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(host), "host is required");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "path is required");

    this.jmx = Optional.fromNullable(jmx);
    this.host = host;
    this.path = path;
  }

  public String host() {
    return this.host;
  }

  public String path() {
    return this.path;
  }

  public Optional<String> jmx() {
    return this.jmx;
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
