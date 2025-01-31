package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;
import java.util.function.Consumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ProcessProvider;

public class JMeterTestService {

  private final JMeterTestFactory jMeterTestFactory;
  private final Consumer<ProcessProvider> processConsumer;

  @Inject(optional=true)
  @Named("env.JMETER_TEST_JMX")
  String jmxEnv = null;

  @Inject(optional=true)
  @Named("jmeter.test.jmx")
  String jmxProp = null;
  
  @Inject
  JMeterTestService(JMeterTestFactory jMeterTestFactory,
      @ProcessConsumer Consumer<ProcessProvider> processConsumer) {
    this.jMeterTestFactory = jMeterTestFactory;
    this.processConsumer = processConsumer;
  }

  public void start(final Optional<String> jmx, final String host, final Optional<String> path,
    final int concurrency, final int duration, final int rampupTime, final int rampupSteps) {
    this.start(jmx, host, path, concurrency, duration, rampupTime, rampupSteps, System.out, false);
  }

  public void start(final Optional<String> jmx, final String host, final Optional<String> path,
    final int concurrency, final int duration, final int rampupTime, final int rampupSteps,
      final OutputStream outputStream, final boolean closeableOutputStream) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(host), "host is required");

    final JMeterTestConfig jMeterTestConfig = new JMeterTestConfig(getJMX(jmx), host, path.or("/"))
      .concurrency(concurrency).duration(duration).rampupTime(rampupTime).rampupSteps(rampupSteps);
    
    final JMeterTest jMeterTest = this.jMeterTestFactory
      .createWithOutputStream(jMeterTestConfig, outputStream, closeableOutputStream);
    this.start(jMeterTest);
  }

  private void start(final JMeterTest jMeterTest) {
    this.processConsumer.accept(jMeterTest);
  }

  private String getJMX(final Optional<String> jmx) {
    return jmx.or(Optional.fromNullable(jmxEnv)).or(Optional.fromNullable(jmxProp)).orNull();
  }

}
