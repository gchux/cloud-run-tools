package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;

import com.google.common.base.Optional;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import dev.chux.gcp.crun.ConfigService;

import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.process.ProcessOutputFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

import static dev.chux.gcp.crun.jmeter.rest.RunJMeterTestController.DEFAULT_TRACE_ID;

public class JMeterTestImpl implements JMeterTest, Supplier<JMeterTestConfig> {

  private static final Logger logger = LoggerFactory.getLogger(JMeterTestImpl.class);

  private static final String JMETER_BIN = "jmeter";

  private static final CharMatcher CONFIG_SEPARATOR = CharMatcher.anyOf(",;:_-|");
  private static final Splitter CONFIG_SPLITTER = Splitter.on(CONFIG_SEPARATOR).omitEmptyStrings().trimResults();

  private final String version;
  private final String jMeterVersion;

  private final JMeterTestConfig jMeterTestConfig;
  private final Optional<OutputStream> stream;
  private final boolean closeable;
  private final Function<Supplier<JMeterTestConfig>, Optional<Path>> requestFileGenerator;
  private final ProcessOutputFactory processOutputFactory;

  private final Provider<String> jmeterTestDirProvider;
  private final Provider<String> jmeterTestProvider;

  private final AtomicBoolean started;

  @AssistedInject
  public JMeterTestImpl(
    final ConfigService configService,
    final RequestFileGenerator requestFileGenerator,
    final ProcessOutputFactory processOutputFactory,
    @Named("jmeter://jmx.dir") Provider<String> jmeterTestDirProvider,
    @Named("jmeter://test.jmx") Provider<String> jmeterTestProvider,
    @Assisted JMeterTestConfig jMeterTestConfig
  ) {
    this(configService, requestFileGenerator, processOutputFactory, jmeterTestDirProvider, jmeterTestProvider, jMeterTestConfig, absent(), false);
  }

  @AssistedInject
  public JMeterTestImpl(
    final ConfigService configService,
    final RequestFileGenerator requestFileGenerator,
    final ProcessOutputFactory processOutputFactory, 
    @Named("jmeter://jmx.dir") Provider<String> jmeterTestDirProvider,
    @Named("jmeter://test.jmx") Provider<String> jmeterTestProvider,
    @Assisted JMeterTestConfig jMeterTestConfig, 
    @Assisted OutputStream stream
  ) {
    this(configService, requestFileGenerator, processOutputFactory, jmeterTestDirProvider, jmeterTestProvider, jMeterTestConfig, fromNullable(stream), false);
  }

  @AssistedInject
  public JMeterTestImpl(
    final ConfigService configService,
    final RequestFileGenerator requestFileGenerator,
    final ProcessOutputFactory processOutputFactory, 
    @Named("jmeter://jmx.dir") Provider<String> jmeterTestDirProvider,
    @Named("jmeter://test.jmx") Provider<String> jmeterTestProvider,
    @Assisted JMeterTestConfig jMeterTestConfig, 
    @Assisted OutputStream stream, 
    @Assisted boolean closeable
  ) {
    this(configService, requestFileGenerator, processOutputFactory, jmeterTestDirProvider, jmeterTestProvider, jMeterTestConfig, fromNullable(stream), closeable);
  }

  public JMeterTestImpl(
    final ConfigService configService,
    final RequestFileGenerator requestFileGenerator,
    final ProcessOutputFactory processOutputFactory,
    final Provider<String> jmeterTestDirProvider,
    final Provider<String> jmeterTestProvider,
    final JMeterTestConfig jMeterTestConfig,
    final Optional<OutputStream> stream,
    final boolean closeable
  ) {
    this.version = this.getVersion(configService);
    this.jMeterVersion = this.getJMeterVersion(configService);

    this.jMeterTestConfig = jMeterTestConfig;
    this.stream = stream;
    this.closeable = closeable;
    this.requestFileGenerator = requestFileGenerator;
    this.processOutputFactory = processOutputFactory;
    this.started = new AtomicBoolean(false);

    this.jmeterTestDirProvider = jmeterTestDirProvider;
    this.jmeterTestProvider = jmeterTestProvider;
  }

  @Override
  public String toString() {
    return toStringHelper(this).addValue(this.get()).toString();
  }

  @Override
  public JMeterTestConfig get() {
    return this.jMeterTestConfig;
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

  private String getVersion(
    final ConfigService configService
  ) {
    return configService.getAppProp("version");
  }

  private String getJMeterVersion(
    final ConfigService configService
  ) {
    return configService.getAppProp("jmeter.version");
  }

  private final List<String> command() {
    final ImmutableList.Builder<String> cmd = ImmutableList.<String>builder();

    final String jmx = this.jmx();

    logger.debug("JMX: {}", jmx);

    this.setID(
      cmd.add(JMETER_BIN, "-n",
        "-l", "/dev/stdout",
        "-j", "/dev/stdout",
        "-t", jmx)
      )
      .setProto(cmd)
      .setMethod(cmd)
      .setHost(cmd)
      .setPath(cmd)
      .setPort(cmd)
      .setConfig(cmd)
      .setProperties(cmd)
      .setVersion(cmd)
      .setJMeterVersion(cmd)
      .setTraceID(cmd)
      .setInstanceID(cmd)
      .setRequestTimeout(cmd)
      .setRequestFile(cmd);

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

  private final JMeterTestImpl setID(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "tid", this.jMeterTestConfig.id());
  }

  private final JMeterTestImpl setHost(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "host", this.host());
  }

  private final JMeterTestImpl setPath(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "path", this.path());
  }

  private final JMeterTestImpl setProto(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "proto", this.proto());
  }

  private final JMeterTestImpl setMethod(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "http_method", this.method());
  }

  private final JMeterTestImpl setPort(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setIntProperty(cmd, "port", this.port());
  }

  private final JMeterTestImpl setTraceID(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "trace_id", this.traceID());
  }

  private final JMeterTestImpl setInstanceID(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "instance_id",
      this.jMeterTestConfig.instanceID());
  }

  private final JMeterTestImpl setConfig(
    final ImmutableList.Builder<String> cmd
  ) {
    if ( this.mode().equalsIgnoreCase("qps") ) {
      return this.setProfile(cmd);
    }
    return this.setProperty(cmd, "threads_schedule", this.threads());
  }

  private final JMeterTestImpl setVersion(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "jmaas_version", this.version);
  }

  private final JMeterTestImpl setJMeterVersion(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setProperty(cmd, "jm_version", this.jMeterVersion);
  }

  private final JMeterTestImpl setRequestTimeout(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setIntProperty(cmd, "request_timeout", this.requestTimeout());
  }

  private final JMeterTestImpl setRequestFile(
    final ImmutableList.Builder<String> cmd
  ) {
    final Optional<Path> requestFilePath = this.requestFileGenerator.apply(this);
    if ( requestFilePath.isPresent() ) {
      final Path path = requestFilePath.get();
      return this.setProperty(cmd, "request_file", path.toString());
    }
    return this;
  }

  private final JMeterTestImpl setProperties(
    final ImmutableList.Builder<String> cmd
  ) {
    return this.setIntProperty(cmd, "concurrency", this.jMeterTestConfig.concurrency())
      .setIntProperty(cmd, "duration", this.jMeterTestConfig.duration())
      .setIntProperty(cmd, "rampup_time", this.jMeterTestConfig.rampupTime())
      .setIntProperty(cmd, "rampup_steps", this.jMeterTestConfig.rampupSteps());
  }

  private final String mode() {
    return this.jMeterTestConfig.mode().toLowerCase();
  }

  private final String host() {
    String host = this.jMeterTestConfig.host();
    if (host.startsWith("http:") || host.startsWith("https:")) {
      host = host.replaceFirst("^https?://", "");
    }
    return CharMatcher.is('/').trimTrailingFrom(host);
  }

  private final String proto() {
    return this.jMeterTestConfig.proto().or("https").toLowerCase();
  }

  private final String method() {
    return this.jMeterTestConfig.method().or("get").toUpperCase();
  }

  private final int port() {
    return this.jMeterTestConfig.port()
      .or(Integer.valueOf(443)).intValue();
  }

  private final String path() {
    return this.jMeterTestConfig.path().or("/");
  }

  private final String jmx() {
    return this.jmeterTestDirProvider.get() + "/" +
      this.jMeterTestConfig.jmx()
        .or(this.jmeterTestProvider.get()) + ".jmx";
  }

  private final String traceID() {
    return this.jMeterTestConfig.traceID().or(DEFAULT_TRACE_ID);
  }

  private final int requestTimeout() {
    return this.jMeterTestConfig.maxLatency() + 1000;
  }

  private final JMeterTestImpl setProfile(
    final ImmutableList.Builder<String> cmd
  ) {
    final Optional<String> profile = this.jMeterTestConfig.profile();
    if (!profile.isPresent()) {
      return this;
    }
    final String value = profile.get();
    return this.setProfile(cmd, CONFIG_SPLITTER.splitToList(value));
  }

  private final int maxQPS(
    int maxQPS, int qps
  ) {
    if ( qps < 0 ) {
      return maxQPS;
    }
    return Math.max(maxQPS, qps);
  }

  private final JMeterTestImpl setProfile(
    final ImmutableList.Builder<String> cmd,
    final List<String> profile
  ) {
    final int sizeOfProfile = profile.size();

    if ((sizeOfProfile%3) != 0) {
      // profile must be in groups of 3
      // profile is incomplete/truncated
      return this;
    }

    int index = 0;
    int duration = 0;
    int lastQPS = 0;
    int maxQPS = 0;

    final StringBuilder loadProfile = new StringBuilder();
    while ( index < sizeOfProfile ) {
      final int i = index%3;
      switch ( i ) {
        case 0: 
        case 1: 
        default: {
          String value = profile.get(index);
          final Optional<Integer> qps = fromNullable(Ints.tryParse(value));
          if ( !qps.isPresent() ) {
            logger.error("invalid qps at profile[{}] = '{}'", index+1, value);
            return this;
          }
          int intValue = qps.get().intValue();
          if ( intValue <= 0 ) {
            value = "1";
            intValue = 1;
          }
          maxQPS = this.maxQPS(maxQPS, intValue);
          if ( i == 0 ) {
            loadProfile.append("line(");
          } else {
            lastQPS = intValue;
          }
          loadProfile.append(value).append(',');
          break;
        }

        case 2: {
          final String value = profile.get(index);
          final Integer D = Ints.tryParse(value);
          if ( D == null ) {
            logger.error("invalid duration at profile[{}] = '{}' | must be a number", index+1, value);
            loadProfile.append("0s) ");
          } else {
            final int d = D.intValue();
            if ( d > 0 ) {
              duration += d;
              loadProfile.append(d).append("s) ");
            } else {
              logger.error("invalid duration at profile[{}] = '{}' | must be a positive integer", index+1, value);
              loadProfile.append("0s) ");
            }
          }
          break;
        }
      }
      index += 1;
    }

    if ( duration <= 0 ) {
      logger.error("produced invalid 'duration': {}", duration);
      return this;
    }

    if ( this.jMeterTestConfig.duration() != duration ) {
      logger.error("invalid 'duration': {} != {}", duration, this.jMeterTestConfig.duration());
      return this;
    }

    if ( maxQPS <= 0 ) {
      logger.error("produced invalid max QPS: {}", maxQPS);
      return this;
    }

    if ( lastQPS > 0 ) {
      // `1s` to drop QPS to 0
      loadProfile.append("line(").append(lastQPS).append(",0,1s)");
      duration += 1;
    } else {
      // delete last space ( trim-right )
      loadProfile.deleteCharAt(loadProfile.length()-1);
    }

    // see: https://jmeter-plugins.org/wiki/ThroughputShapingTimer/#How-Many-Threads-I-Need-To-Produce-Desired-RPS
    // lit: `RPS * <max response time> / 1000`
    final int threads = (maxQPS*this.jMeterTestConfig.maxLatency())/1000;

    final String threadsStr = Integer.toString(50*threads, 10);
    final String durationStr = Integer.toString(duration, 10);

    final String threadsSchedule = "spawn(" + threadsStr + ",0s,0s," + durationStr + "s,1s)";

    return this.setProperty(cmd, "threads_schedule", threadsSchedule)
                .setProperty(cmd, "load_profile", loadProfile.toString());
  }

  private final String threads() {
    final Optional<String> threads = this.jMeterTestConfig.threads();
    if (!threads.isPresent()) {
      return "spawn(0,0s,0s,0s,0s)";
    }
    final String t = threads.get();
    return this.threads(CONFIG_SPLITTER.splitToList(t));
  }

  private final String threads(
    final List<String> schedule
  ) {
    final int sizeOfConfig = schedule.size();

    if ( (sizeOfConfig%5) != 0 ) {
      // threads must be in groups of 5
      // threads argument is incomplete/truncated
      return "spawn(0,0s,0s,0s,0s)";
    }

    int index = 0;
    int duration = 0;

    final StringBuilder threadsSchedule = new StringBuilder();

    while ( index < sizeOfConfig ) {
      final String value = schedule.get(index);

      final Integer v = Ints.tryParse(value);

      if ( (v == null) || (v.intValue() <= 0) ) {
        logger.error("invalid value at threads[{}] = '{}'", index+1, value);
        return "spawn(0,0s,0s,0s,0s)";
      }

      final int i = index%5;

      if ( i == 0 ) {
        threadsSchedule
          .append("spawn(")
          .append(value);
      } else {
        threadsSchedule
          .append(',')
          .append(value)
          .append('s');
      }

      if ( i >= 2 ) {
        duration += v.intValue();
      }

      if ( (++index%5) == 0 ) {
        threadsSchedule.append(") ");
      }
    }

    if ( duration <= 0 ) {
      logger.error("produced invalid 'duration': {}", duration);
      return "spawn(0,0s,0s,0s,0s)";
    }

    if ( this.jMeterTestConfig.duration() != duration ) {
      logger.error("invalid 'duration': {} != {}", duration, this.jMeterTestConfig.duration());
      return "spawn(0,0s,0s,0s,0s)";
    }

    // delete last space ( trim-right )
    threadsSchedule.deleteCharAt(
      threadsSchedule.length()-1
    );
    return threadsSchedule.toString();
  }

}
