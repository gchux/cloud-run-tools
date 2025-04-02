package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

import java.util.Map;

import com.google.common.base.Optional;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class JMeterTestWatchdog implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(JMeterTestWatchdog.class);

  private final Map<String, JMeterTest> jMeterTestStorage;
  private final JMeterTest test;

  @AssistedInject
  private JMeterTestWatchdog(
    final Map<String, JMeterTest> jMeterTestStorage,
    @Assisted JMeterTest test
  ) {
    this.jMeterTestStorage = jMeterTestStorage;
    this.test = checkNotNull(test);
    checkState(!isNullOrEmpty(test.id()));
  }

  private void flush(
    final String testID,
    final OutputStream stream
  ) {
    try {
      stream.flush();
      logger.info("{} | flushed stream '{}': {}", testID, stream);
    } catch(final Exception e) {
      logger.error(
        "{} | failed to flush '{}' =>\n{}",
        testID, stream, getStackTraceAsString(e)
      );
    }
  }

  private void flush(
    final JMeterTest test
  ) {
    final String id = test.id();

    if ( !this.jMeterTestStorage.containsKey(id) ) {
      logger.warn("test not found: {}", test);
      return;
    }

    final Optional<OutputStream> stream = test.stream();
    if ( stream.isPresent() ) {
      this.flush(id, stream.get());
    }
  }

  @Override
  public void run() {
    checkState(!isNullOrEmpty(test.id()));
    this.flush(this.test);
  }

}
