package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

import java.util.Map;
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

  public void start(final String instanceID,
    final String id, final Optional<String> traceID, final Optional<String> jmx, final String mode,
    final Optional<String> proto, final Optional<String> method, final String host, final Optional<Integer> port,
    final Optional<String> path, final Map<String, String> query, final Map<String, String> headers,
    final Optional<String> body, final Optional<String> threads, final Optional<String> profile,
    final int concurrency, final int duration, final int rampupTime, final int rampupSteps,
    final int minLatency, final int maxLatency) {
    this.start(instanceID, id, traceID, jmx, mode, proto, method, host, port, path, query, headers, body,
      threads, profile, concurrency, duration, rampupTime, rampupSteps, System.out, false, minLatency, maxLatency);
  }

  public void start(final String instanceID,
    final String id, final Optional<String> traceID, final Optional<String> jmx, final String mode,
    final Optional<String> proto, final Optional<String> method, final String host, final Optional<Integer> port,
    final Optional<String> path, final Map<String, String> query, final Map<String, String> headers,
    final Optional<String> body, final Optional<String> threads, final Optional<String> profile,
    final int concurrency, final int duration, final int rampupTime, final int rampupSteps,
    final OutputStream outputStream, final boolean closeableOutputStream,
    final int minLatency, final int maxLatency) {

    checkArgument(!isNullOrEmpty(instanceID), "instanceID is required");
    checkArgument(!isNullOrEmpty(id), "ID is required");
    checkArgument(!isNullOrEmpty(host), "host is required");
    checkArgument(!isNullOrEmpty(mode), "mode is required");

    final JMeterTestConfig config = new JMeterTestConfig(
      instanceID, id, this.jmx(jmx), mode, proto.orNull(),
      method.orNull(), host, port.orNull(), path.orNull(),
      query, headers, body.orNull(), minLatency, maxLatency
    ).traceID(traceID.orNull())
      .threads(threads.orNull()).profile(profile.orNull())
      .concurrency(concurrency).duration(duration)
      .rampupTime(rampupTime).rampupSteps(rampupSteps);

    final JMeterTest test = this.newJMeterTest(config, outputStream, closeableOutputStream);
    this.processConsumer.accept(test);
  }

  private final String jmx(final Optional<String> jmx) {
    return jmx.or(this.jmeterTestProvider.get());
  }

  private final JMeterTest newJMeterTest(
    final JMeterTestConfig config,
    final OutputStream stream,
    final boolean closeable
  ) {
    return this.jMeterTestFactory.createWithOutputStream(config, stream, closeable);

  }

}
