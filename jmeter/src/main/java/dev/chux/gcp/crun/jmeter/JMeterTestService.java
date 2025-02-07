package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;
import java.util.function.Consumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import com.google.common.base.Optional;

import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ProcessProvider;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class JMeterTestService {

  private final JMeterTestFactory jMeterTestFactory;
  private final Consumer<ProcessProvider> processConsumer;
  private final Provider<String> jmeterTestProvider;
  
  @Inject
  JMeterTestService(JMeterTestFactory jMeterTestFactory,
    @ProcessConsumer Consumer<ProcessProvider> processConsumer,
    @Named("jmeter://test.jmx") Provider<String> jmeterTestProvider) {
    this.jMeterTestFactory = jMeterTestFactory;
    this.processConsumer = processConsumer;
    this.jmeterTestProvider = jmeterTestProvider;
  }

  public void start(final Optional<String> jmx, final String host, final Optional<String> path,
    final int concurrency, final int duration, final int rampupTime, final int rampupSteps) {
    this.start(jmx, host, path, concurrency, duration, rampupTime, rampupSteps, System.out, false);
  }

  public void start(final Optional<String> jmx, final String host, final Optional<String> path,
    final int concurrency, final int duration, final int rampupTime, final int rampupSteps,
      final OutputStream outputStream, final boolean closeableOutputStream) {

    checkArgument(!isNullOrEmpty(host), "host is required");

    final JMeterTestConfig config = new JMeterTestConfig(this.jmx(jmx), host, path.orNull())
      .concurrency(concurrency).duration(duration).rampupTime(rampupTime).rampupSteps(rampupSteps);

    final JMeterTest test = this.newJMeterTest(config, outputStream, closeableOutputStream);

    this.start(test);
  }

  private final void start(final JMeterTest jMeterTest) {
    this.processConsumer.accept(jMeterTest);
  }

  private final String jmx(final Optional<String> jmx) {
    return jmx.or(this.jmeterTestProvider.get());
  }

  private final JMeterTest newJMeterTest(
    final JMeterTestConfig config, final OutputStream stream, final boolean closeable) {
    return this.jMeterTestFactory
      .createWithOutputStream(config, stream, closeable);

  }

}
