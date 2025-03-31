package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.ListenableFuture;

import dev.chux.gcp.crun.io.ProxyOutputStream;
import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ProcessProvider;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.util.concurrent.Futures.immediateFuture;

public class JMeterTestService {

  private static final Logger logger = LoggerFactory.getLogger(JMeterTestService.class);

  private final JMeterTestFactory jMeterTestFactory;
  private final Consumer<ProcessProvider> processConsumer;
  private final Provider<String> jmeterTestProvider;
  private final Map<String, JMeterTest> jmeterTestStorage;

  private final Map<String, ProxyOutputStream> streams = Maps.newConcurrentMap();
  private final Map<String, ListenableFuture<JMeterTest>> tests = Maps.newConcurrentMap();

  private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
  private final ListeningScheduledExecutorService scheduledExecutor = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));

  private static class Watchdog implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Watchdog.class);

    private final Map<String, JMeterTest> jMeterTestStorage;

    private Watchdog(
      final Map<String, JMeterTest> jmeterTestStorage
    ) {
      this.jMeterTestStorage = jmeterTestStorage;
    }

    private boolean flush(
      final JMeterTest test
    ) {
      final Optional<OutputStream> stream = test.stream();
      if ( stream.isPresent() ) {
        final OutputStream s = stream.get();
        try {
          s.flush();
          logger.info(
            "{} | flushed stream '{}': {}", test.id(), s
          );
          return true;
        } catch(final Exception e) {
          logger.error(
            "{} | failed to flush '{}' =>\n{}",
            test.id(), s, getStackTraceAsString(e)
          );
        }
      }
      return false;
    }

    @Override
    public void run() {
      if ( this.jMeterTestStorage.isEmpty() ) {
        // no-op
        return;
      }
      int flushedStreams = 0;
      for ( final Map.Entry<String, JMeterTest> test :
            this.jMeterTestStorage.entrySet() ) {
        if ( this.flush(test.getValue()) ) {
           flushedStreams += 1;
        }
      }
      logger.info("flushed {} streams", flushedStreams);
    }

  }

  private static class TestExecutor implements Callable<JMeterTest> {

    private static final Logger logger = LoggerFactory.getLogger(Executor.class);

    private final Consumer<ProcessProvider> processConsumer;
    private final JMeterTest test;

    private TestExecutor(
      @ProcessConsumer
        final Consumer<
          ProcessProvider
        > processConsumer,
      final JMeterTest test
    ) {
      this.processConsumer = processConsumer;
      this.test = test;
    }

    private void clockIn(
      final JMeterTestConfig config
    ) {
      config.started(System.currentTimeMillis());
    }

    private void clockOut(
      final JMeterTestConfig config
    ) {
      config.finished(System.currentTimeMillis());
    }

    @Override
    public JMeterTest call() {
      final JMeterTestConfig config = this.test.get();
      logger.info("starting test: {}", this.test);
      this.clockIn(config);
      this.processConsumer.accept(this.test);
      this.clockOut(config);
      logger.info("test complete: {}", this.test);
      return this.test;
    }

  }
  
  @Inject
  JMeterTestService(
    final JMeterTestFactory jMeterTestFactory,
    @ProcessConsumer final Consumer<ProcessProvider> processConsumer,
    @Named("jmeter://test.jmx") final Provider<String> jmeterTestProvider,
    final Map<String, JMeterTest> jmeterTestStorage
  ) {
    this.jMeterTestFactory = jMeterTestFactory;
    this.processConsumer = processConsumer;
    this.jmeterTestProvider = jmeterTestProvider;
    this.jmeterTestStorage = jmeterTestStorage;

    this.scheduledExecutor.scheduleAtFixedRate(
      new Watchdog(jmeterTestStorage),
      3l, 3l, TimeUnit.SECONDS
    );
  }

  public final ListenableFuture<JMeterTest> start(
    final FutureCallback<JMeterTest> callback, final String instanceID,
    final String id, final Optional<String> traceID, final Optional<String> jmx, final String mode,
    final Optional<String> proto, final Optional<String> method, final String host, final Optional<Integer> port,
    final Optional<String> path, final Map<String, String> query, final Map<String, String> headers,
    final Optional<String> body, final Optional<String> threads, final Optional<String> profile,
    final int concurrency, final int duration, final int rampupTime, final int rampupSteps,
    final int minLatency, final int maxLatency
  ) {
    return this.start(callback, instanceID, id, traceID, jmx, mode, proto, method, host, port, path, query, headers, body,
      threads, profile, concurrency, duration, rampupTime, rampupSteps, System.out, false, minLatency, maxLatency);
  }

  public final ListenableFuture<JMeterTest> start(
    final FutureCallback<JMeterTest> callback, final String instanceID,
    final String id, final Optional<String> traceID, final Optional<String> jmx, final String mode,
    final Optional<String> proto, final Optional<String> method, final String host, final Optional<Integer> port,
    final Optional<String> path, final Map<String, String> query, final Map<String, String> headers,
    final Optional<String> body, final Optional<String> threads, final Optional<String> profile,
    final int concurrency, final int duration, final int rampupTime, final int rampupSteps,
    final OutputStream outputStream, final boolean closeableOutputStream,
    final int minLatency, final int maxLatency
  ) {

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
    .threads(threads.orNull())
    .profile(profile.orNull())
    .concurrency(concurrency)
    .duration(duration)
    .rampupTime(rampupTime)
    .rampupSteps(rampupSteps);

    final OutputStream teeStream = this.wrapStream(config, outputStream);
    final JMeterTest test = this.newJMeterTest(config, teeStream, closeableOutputStream);

    final Optional<JMeterTest> t = fromNullable(
      this.jmeterTestStorage.putIfAbsent(id, test)
    );
    if ( t.isPresent() ) {
      logger.warn("test '{}' is already running", t.get());
      return this.test(id).or(immediateFuture(t.get()));
    }

    // start JMeter test asynchronously
    final ListenableFuture<JMeterTest> futureTest =
      this.executor.submit(
        new TestExecutor(this.processConsumer, test)
      );

    // clean up after test execution is complete
    Futures.<JMeterTest>addCallback(
      futureTest,
      new FutureCallback<JMeterTest>() {
        private void always(
          final boolean success,
          final Optional<JMeterTest> t,
          final Optional<Throwable> e
        ) {
          if ( success ) {
            JMeterTestService.this.logger.info(
              "test complete: {}", t
            );
            JMeterTestService.this.clean(t.get());
          } else {
            JMeterTestService.this.logger.error(
              "test failed: {} =>\n{}", test,
              path, getStackTraceAsString(e.get())
            );
            JMeterTestService.this.clean(test);
          }
        }
        public void onSuccess(final JMeterTest t) {
          this.always(/* success */ true, fromNullable(t), absent());
        }
        public void onFailure(final Throwable t) {
          this.always(/* success */ false, absent(), fromNullable(t));
        }
      },
      this.executor
    );
    Futures.<JMeterTest>addCallback(futureTest, callback, this.executor);

    // save a reference to this test's `Future`
    final ListenableFuture<
      JMeterTest
    > producedValue =
      fromNullable(
        this.tests.putIfAbsent(
          id, futureTest
        )
      ).or(futureTest);

    logger.info("> {}", this.toString());

    return producedValue;
  }

  public final Executor executor(
    final String id
  ) {
    final Optional<
      ListenableFuture<
        JMeterTest
      >
    > test = this.test(id);
    if ( test.isPresent() ) {
      return this.executor;
    }
    return MoreExecutors.directExecutor();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .addValue(this.jmeterTestStorage)
      .add("tests", this.tests)
      .add("streams", this.streams)
      .toString();
  }

  public final Optional<
    JMeterTest
  > get(final String id) {
    checkArgument(!isNullOrEmpty(id));
    return fromNullable(
      this.jmeterTestStorage.get(id)
    );
  }

  public final Optional<
    ListenableFuture<
      JMeterTest
    >
  > getTest(final String id) {
    final Optional<
      ListenableFuture<JMeterTest>
    > test = this.test(id);
    if ( test.isPresent() ) {
      // return a non-cancelable `Future`
      return Optional.of(
        Futures.nonCancellationPropagating(
          test.get()
        )
      );
    }
    return absent();
  }

  private Optional<
    ListenableFuture<
      JMeterTest
    >
  > test(final String id) {
    checkArgument(!isNullOrEmpty(id));
    return fromNullable(this.tests.get(id));
  }

  private Optional<
    ProxyOutputStream
  > stream(final String id) {
    checkArgument(!isNullOrEmpty(id));
    return fromNullable(this.streams.get(id));
  }

  public final Optional<
    ListenableFuture<
      JMeterTest
    >
  > connect(
    final String id,
    final OutputStream stream
  ) throws Exception {
    final Optional<
      ListenableFuture<
        JMeterTest
      >
    > test = this.test(id);
    if ( test.isPresent() ) {
      final Optional<
        ProxyOutputStream
      > s = this.stream(id);
      if ( s.isPresent() ) {
        s.get().setReference(stream).flush();
        
        logger.info("connected to test: {}", test.get());

        // return a non-cancelable `Future`:
        //   prevent unexpected/unwanted cancelations.
        return Optional.of(
          Futures.nonCancellationPropagating(
            test.get()
          )
        );
      }
    }
    return absent();
  }

  public final Optional<
    ListenableFuture<
      JMeterTest
    >
  > connect(
    final JMeterTest test,
    final OutputStream stream
  ) throws Exception {
    return this.connect(test.id(), stream);
  }

  private final String jmx(
    final Optional<String> jmx
  ) {
    return jmx.or(this.jmeterTestProvider.get());
  }

  private OutputStream wrapStream(
    final JMeterTestConfig config,
    final OutputStream stream
  ) {
    // tee output to a proxy stream to allow other threads to connect to it.
    //   the default instance of `ProxyOutputStream` is backed by `NullOutputStream`,
    //   so as long as no threads connect to a test's output stream, it tees into void.
    final ProxyOutputStream proxyStream = new ProxyOutputStream();
    final OutputStream teeStream = new TeeOutputStream(stream, proxyStream);
    this.streams.putIfAbsent(config.id(), proxyStream);
    return teeStream;
  }

  private final JMeterTest newJMeterTest(
    final JMeterTestConfig config,
    final OutputStream stream,
    final boolean closeable
  ) {
    return this.jMeterTestFactory
      .createWithOutputStream(config, stream, closeable);
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

  private final void clean(
    final JMeterTest test
  ) {
    final String id = test.id();
    final String name = test.name();

    final Path[] paths = new Path[] {
      Paths.get("/tmp/" + name),
      Paths.get("/tmp/" + name + "_body")
    };

    for ( final Path path : paths ) {
      try {
        if ( Files.deleteIfExists(path) ) {
          logger.info("{}/deleted: {}", id, path);
        }
      } catch(final Exception e) {
        logger.error(
          "{}/failed to delete '{}' =>\n{}",
          id, path, getStackTraceAsString(e)
        );
      }
    }

    final Optional<ProxyOutputStream> stream =
      fromNullable(this.streams.remove(id));
    if ( stream.isPresent() ) {
      final ProxyOutputStream s = stream.get();
      try {
        s.flush();
      } catch(final Exception e) {
        logger.error(
          "{}/failed to flush '{}' =>\n{}",
          id, s, getStackTraceAsString(e)
        );
      }
      s.setReference(NullOutputStream.INSTANCE);
    }
    this.tests.remove(id);
    this.jmeterTestStorage.remove(id, test);

    logger.info("< {}", this.toString());
  }

}
