package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Map;
import java.util.function.Consumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import com.google.common.base.Optional;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ProcessProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class JMeterTestService {

  private static final Logger logger = LoggerFactory.getLogger(JMeterTestService.class);

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

    final String JMX = this.jmx(jmx);
    final String name = this.name(instanceID, id, JMX);

    final JMeterTestConfig config = new JMeterTestConfig(
      name, instanceID, id, JMX, mode, proto.orNull(),
      method.orNull(), host, port.orNull(), path.orNull(),
      query, headers, body.orNull(), minLatency, maxLatency
    )
    .traceID(traceID.orNull())
    .threads(threads.orNull()).profile(profile.orNull())
    .concurrency(concurrency).duration(duration)
    .rampupTime(rampupTime).rampupSteps(rampupSteps);

    final JMeterTest test = this.newJMeterTest(config, outputStream, closeableOutputStream);

    logger.info("starting test: {}", test);

    this.processConsumer.accept(test);
    this.clean(name);
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

  private String name(
    final String instanceID,
    final String id,
    final String jmx
  ) {
    return Hashing.crc32c()
      .newHasher()
      .putString(instanceID, UTF_8)
      .putString(id, UTF_8)
      .putString(jmx, UTF_8)
      .putLong(System.currentTimeMillis())
      .hash()
      .toString();
  }

  private final void clean(final String name) {
    final Path[] paths = new Path[] {
      Paths.get("/tmp/" + name),
      Paths.get("/tmp/" + name + "_body")
    };
    for ( final Path path : paths ) {
      try {
        if ( Files.deleteIfExists(path) ) {
          logger.info("deleted: {}", path);
        }
      } catch(final Exception e) {
        logger.error("failed to delete '{}': {}", path, getStackTraceAsString(e));
      }
    }
  }

}
