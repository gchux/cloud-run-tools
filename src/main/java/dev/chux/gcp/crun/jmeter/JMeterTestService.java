package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

import com.google.inject.Inject;
import java.util.function.Consumer;

import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ProcessProvider;

public class JMeterTestService {

  private final JMeterTestFactory jMeterTestFactory;
  private final Consumer<ProcessProvider> processConsumer;
  
  @Inject
  JMeterTestService(JMeterTestFactory jMeterTestFactory,
      @ProcessConsumer Consumer<ProcessProvider> processConsumer) {
    this.jMeterTestFactory = jMeterTestFactory;
    this.processConsumer = processConsumer;
  }

  public void start(final String host, final String path, final int concurrency, 
      final int duration, final int rampupTime, final int rampupSteps) {
    this.start(host, path, concurrency, duration, rampupTime, rampupSteps, System.out, false);
  }

  public void start(final String host, final String path, final int concurrency, 
      final int duration, final int rampupTime, final int rampupSteps,
      final OutputStream outputStream, final boolean closeableOutputStream) {
    final JMeterTestConfig jMeterTestConfig = new JMeterTestConfig(host, path)
      .concurrency(concurrency).duration(duration).rampupTime(rampupTime).rampupSteps(rampupSteps);
    final JMeterTest jMeterTest = this.jMeterTestFactory
      .createWithOutputStream(jMeterTestConfig, outputStream, closeableOutputStream);
    this.start(jMeterTest);
  }

  private void start(final JMeterTest jMeterTest) {
    this.processConsumer.accept(jMeterTest);
  }

}
