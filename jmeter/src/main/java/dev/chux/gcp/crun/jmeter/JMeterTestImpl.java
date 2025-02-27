package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;

import com.google.common.base.Optional;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.process.ProcessOutputFactory;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class JMeterTestImpl implements JMeterTest {

  private static final String JMETER_BIN = "jmeter";

  private final JMeterTestConfig jMeterTestConfig;
  private final Optional<OutputStream> stream;
  private final boolean closeable;
  private final ProcessOutputFactory processOutputFactory;

  private final Provider<String> jmeterTestDirProvider;
  private final Provider<String> jmeterTestProvider;

  private final AtomicBoolean started;

  @AssistedInject
  public JMeterTestImpl(ProcessOutputFactory processOutputFactory, 
    @Named("jmeter://jmx.dir") Provider<String> jmeterTestDirProvider,
    @Named("jmeter://test.jmx") Provider<String> jmeterTestProvider,
    @Assisted JMeterTestConfig jMeterTestConfig) {
    this(processOutputFactory, jmeterTestDirProvider, jmeterTestProvider, jMeterTestConfig, absent(), false);
  }

  @AssistedInject
  public JMeterTestImpl(ProcessOutputFactory processOutputFactory, 
    @Named("jmeter://jmx.dir") Provider<String> jmeterTestDirProvider,
    @Named("jmeter://test.jmx") Provider<String> jmeterTestProvider,
    @Assisted JMeterTestConfig jMeterTestConfig, 
    @Assisted OutputStream stream) {
    this(processOutputFactory, jmeterTestDirProvider, jmeterTestProvider, jMeterTestConfig, fromNullable(stream), false);
  }

  @AssistedInject
  public JMeterTestImpl(ProcessOutputFactory processOutputFactory, 
    @Named("jmeter://jmx.dir") Provider<String> jmeterTestDirProvider,
    @Named("jmeter://test.jmx") Provider<String> jmeterTestProvider,
    @Assisted JMeterTestConfig jMeterTestConfig, 
    @Assisted OutputStream stream, 
    @Assisted boolean closeable) {
    this(processOutputFactory, jmeterTestDirProvider, jmeterTestProvider, jMeterTestConfig, fromNullable(stream), closeable);
  }

  public JMeterTestImpl(ProcessOutputFactory processOutputFactory,
    Provider<String> jmeterTestDirProvider,
    Provider<String> jmeterTestProvider,
    JMeterTestConfig jMeterTestConfig,
    Optional<OutputStream> stream,
    boolean closeable) {
    this.jMeterTestConfig = jMeterTestConfig;
    this.stream = stream;
    this.closeable = closeable;
    this.processOutputFactory = processOutputFactory;
    this.started = new AtomicBoolean(false);

    this.jmeterTestDirProvider = jmeterTestDirProvider;
    this.jmeterTestProvider = jmeterTestProvider;
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
      .add(JMETER_BIN, "-n",
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
    return this.jmeterTestDirProvider.get() + "/" +
      this.jMeterTestConfig.jmx()
        .or(this.jmeterTestProvider.get()) + ".jmx";
  }

}
