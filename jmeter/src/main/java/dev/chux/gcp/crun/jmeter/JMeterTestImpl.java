package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import com.google.common.base.Optional;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

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
    return new ProcessBuilder(command()).redirectErrorStream(true);
  } 

  @Override
  public ProcessOutput getOutput() {
    if( this.stream.isPresent() ) {
      return this.processOutputFactory.create(this.stream.get(), this.closeable);
    }
    return this.processOutputFactory.create(System.out, /* closeable */ false);
  }

  private final List<String> command() {
    final ImmutableList.Builder<String> cmd = ImmutableList.<String>builder();
    this.setHost(cmd
      .add("jmeter", "-n",
        "-l", "/dev/stdout",
        "-j", "/dev/stdout",
        "-t", this.jmx()))
      .setPath(cmd)
      .setProperties(cmd);
    return cmd.build();
  }

  private final JMeterTestImpl setProperty(
    final ImmutableList.Builder<String> cmd, final String key, final String value) {
    cmd.add("-J" + key + "=" + value);
    return this;
  }

  private final JMeterTestImpl setIntProperty(
    final ImmutableList.Builder<String> cmd, final String key, final int value) {
      return this.setProperty(cmd, key, Integer.toString(value, 10));
  }

  private final JMeterTestImpl setHost(final ImmutableList.Builder<String> cmd) {
    return this.setProperty(cmd, "host", this.host());
  }

  private final JMeterTestImpl setPath(final ImmutableList.Builder<String> cmd) {
    return this.setProperty(cmd, "path", this.path());
  }

  private final JMeterTestImpl setProperties(final ImmutableList.Builder<String> cmd) {
    return this.setIntProperty(cmd, "concurrency", this.jMeterTestConfig.concurrency())
      .setIntProperty(cmd, "duration", this.jMeterTestConfig.duration())
      .setIntProperty(cmd, "rampup_time", this.jMeterTestConfig.rampupTime())
      .setIntProperty(cmd, "rampup_steps", this.jMeterTestConfig.rampupSteps());
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
      this.jMeterTestConfig.jmx()
        .or(DEFAULT_JMETER_JMX) + ".jmx";
  }

}
