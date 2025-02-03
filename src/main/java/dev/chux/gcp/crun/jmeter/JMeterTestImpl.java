package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import com.google.common.base.Optional;
import com.google.common.base.CharMatcher;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.process.ProcessOutputFactory;

public class JMeterTestImpl implements JMeterTest {

  private static final String DEFAULT_JMETER_JMX_DIR = "/jmx";
  private static final String DEFAULT_JMETER_JMX = "test";

  private final JMeterTestConfig jMeterTestConfig;
  private final Optional<OutputStream> stream;
  private final boolean closeable;
  private final ProcessOutputFactory processOutputFactory;

  private final AtomicBoolean started;

  @Inject(optional=true)
  @Named("env.JMETER_JMX_DIR")
  String jmxDirEnv = null;

  @Inject(optional=true)
  @Named("jmeter.jmx.dir")
  String jmxDirProp = null;

  @AssistedInject
  public JMeterTestImpl(ProcessOutputFactory processOutputFactory, 
      @Assisted JMeterTestConfig jMeterTestConfig) {
    this(processOutputFactory, jMeterTestConfig, Optional.absent(), false);
  }

  @AssistedInject
  public JMeterTestImpl(ProcessOutputFactory processOutputFactory, 
      @Assisted JMeterTestConfig jMeterTestConfig, 
      @Assisted OutputStream stream) {
    this(processOutputFactory, jMeterTestConfig, Optional.fromNullable(stream), false);
  }

  @AssistedInject
  public JMeterTestImpl(ProcessOutputFactory processOutputFactory, 
      @Assisted JMeterTestConfig jMeterTestConfig, 
      @Assisted OutputStream stream, 
      @Assisted boolean closeable) {
    this(processOutputFactory, jMeterTestConfig, Optional.fromNullable(stream), closeable);
  }

  public JMeterTestImpl(ProcessOutputFactory processOutputFactory,
      JMeterTestConfig jMeterTestConfig, Optional<OutputStream> stream, boolean closeable) {
    this.jMeterTestConfig = jMeterTestConfig;
    this.stream = stream;
    this.closeable = closeable;
    this.processOutputFactory = processOutputFactory;
    this.started = new AtomicBoolean(false);
  }

  @Override
  public ProcessBuilder getBuilder() {
    final ProcessBuilder builder = new ProcessBuilder("jmeter", 
        "-n", 
        "-l", "/dev/stdout",
        "-j", "/dev/stdout",
        "-t", this.jmx(), 
        "-Jhost=" + this.host(), 
        "-Jpath=" + this.path(), 
        "-Jconcurrency=" + Integer.toString(this.jMeterTestConfig.concurrency(), 10), 
        "-Jduration=" + Integer.toString(this.jMeterTestConfig.duration(), 10), 
        "-Jrampup_time=" + Integer.toString(this.jMeterTestConfig.rampupTime(), 10), 
        "-Jrampup_steps=" + Integer.toString(this.jMeterTestConfig.rampupSteps(), 10));
    builder.redirectErrorStream(true);
    return builder;
  } 

  @Override
  public ProcessOutput getOutput() {
    if( this.stream.isPresent() ) {
      return this.processOutputFactory.create(this.stream.get(), this.closeable);
    }
    return this.processOutputFactory.create(System.out, /* closeable */ false);
  }

  private final String host() {
    String host = this.jMeterTestConfig.host();
    if (host.startsWith("https://")) {
      host = host.replaceFirst("^https://", "");
    }
    return CharMatcher.is('/').trimTrailingFrom(host);
  }

  private final String path() {
    return this.jMeterTestConfig.path().or("/");
  }

  private final String jmx() {
    return Optional.fromNullable(this.jmxDirEnv)
      .or(Optional.fromNullable(this.jmxDirProp))
      .or(DEFAULT_JMETER_JMX_DIR) + "/" +
      this.jMeterTestConfig.jmx().or(DEFAULT_JMETER_JMX) + ".jmx";
  }

}
