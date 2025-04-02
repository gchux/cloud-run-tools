package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

import java.util.Map;

import com.google.common.base.Optional;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    this.test = test;
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
      final OutputStream s = stream.get();
      try {
        s.flush();
        logger.info("{} | flushed stream '{}': {}", id, s);
      } catch(final Exception e) {
        logger.error(
          "{} | failed to flush '{}' =>\n{}",
          id, s, getStackTraceAsString(e)
        );
      }
    }
  }

  @Override
  public void run() {
    this.flush(this.test);
  }

}

