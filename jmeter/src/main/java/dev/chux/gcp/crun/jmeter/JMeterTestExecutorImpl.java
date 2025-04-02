
package dev.chux.gcp.crun.jmeter;

import java.util.function.Consumer;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.ListenableScheduledFuture;

import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ProcessProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class JMeterTestExecutorImpl implements JMeterTestExecutor {

  private static final Logger logger = LoggerFactory.getLogger(JMeterTestExecutorImpl.class);

  private static final ListeningScheduledExecutorService EXECUTOR =
    MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(2));

  private final JMeterTestService jMeterTestService;
  private final JMeterTestFactory jMeterTestFactory;
  private final Consumer<ProcessProvider> processConsumer;
  private final JMeterTest test;

  @AssistedInject
  private JMeterTestExecutorImpl(
    final JMeterTestService jMeterTestService,
    final JMeterTestFactory jMeterTestFactory,
    @ProcessConsumer
      final Consumer<
        ProcessProvider
      > processConsumer,
    @Assisted final JMeterTest test
  ) {
    this.jMeterTestService = jMeterTestService;
    this.jMeterTestFactory = jMeterTestFactory;
    this.processConsumer = processConsumer;
    this.test = checkNotNull(test);
    checkState(!isNullOrEmpty(test.id()));
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

  private ListenableScheduledFuture startWatchdog(
    final JMeterTest test
  ) {
    return EXECUTOR.scheduleAtFixedRate(
      this.jMeterTestFactory.createWatchdog(test),
      3l, 3l, TimeUnit.SECONDS
    );
  }

  private void stopWatchdog(
    final ListenableScheduledFuture watchdog,
    final JMeterTest test
  ) {
    if ( !watchdog.isDone() && !watchdog.isCancelled() ) {
      watchdog.cancel(true);
    }
  }

  private void always(
    final boolean success,
    final Optional<JMeterTest> test,
    final Optional<Throwable> error
  ) {
    if ( success ) {
      logger.info(
        "test complete: {}", test
      );
    } else {
      logger.error(
        "test failed: {} =>\n{}", this.get(),
        getStackTraceAsString(error.get())
      );
    }
  }
  
  @Override
  public void onSuccess(final JMeterTest test) {
    this.always(/* success */ true, fromNullable(test), absent());
  }

  @Override
  public void onFailure(final Throwable error) {
    this.always(/* success */ false, absent(), fromNullable(error));
  }

  @Override
  public JMeterTest get() {
    return this.test;
  }

  @Override
  public JMeterTest call() {
    final JMeterTest test = this.get();
    final JMeterTestConfig config = checkNotNull(test.get());
    final ListenableScheduledFuture watchdog = this.startWatchdog(test);
    logger.info("starting test: {}", test);
    this.clockIn(config);
    this.processConsumer.accept(test);
    this.clockOut(config);
    logger.info("test complete: {}", test);
    this.stopWatchdog(watchdog, test);
    // clean up after test execution is complete
    this.jMeterTestService.clean(test);
    return test;
  }

}
