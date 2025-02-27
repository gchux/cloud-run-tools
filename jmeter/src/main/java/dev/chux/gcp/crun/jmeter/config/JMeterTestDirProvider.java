package dev.chux.gcp.crun.jmeter.config;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import com.google.common.base.Supplier;
import com.google.common.base.Optional;

import dev.chux.gcp.crun.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.fromNullable;

public class JMeterTestDirProvider implements Provider<String>, Supplier<String> {
  private static final Logger logger = LoggerFactory.getLogger(JMeterTestDirProvider.class);

  private static final String DEFAULT_JMETER_JMX_DIR = "/jmx";

  private Optional<String> env;
  private Optional<String> prop;

  @Inject(optional=true)
  void setEnv(
    @Named("env.JMETER_JMX_DIR")
    final String env
  ) {
    logger.debug("@Inject [env.JMETER_JMX_DIR={}]", env);
    this.env = fromNullable(env);
  };

  @Inject(optional=true)
  void setProp(
    @Named("jmeter.jmx.dir")
    final String prop
  ) {
    logger.debug("@Inject [jmeter.jmx.dir={}]", prop);
    this.prop = fromNullable(prop);
  };

  @Inject
  JMeterTestDirProvider(
    final ConfigService configService
  ) {
    // intentionally not using `configService`
    //   - dependency is required for its side-effects.
    this.env = configService.getOptionalEnvVar("env.JMETER_JMX_DIR");
    this.prop = configService.getOptionalAppProp("jmeter.jmx.dir");
  }

  @Override
  public String get() {
    logger.debug("env.JMETER_JMX_DIR={}", this.env);
    logger.debug("jmeter.jmx.dir={}", this.prop);
    return this.env.or(this.prop).or(DEFAULT_JMETER_JMX_DIR);
  }

}
