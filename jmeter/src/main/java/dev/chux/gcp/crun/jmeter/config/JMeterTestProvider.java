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

public class JMeterTestProvider implements Provider<String>, Supplier<String> {
  private static final Logger logger = LoggerFactory.getLogger(JMeterTestProvider.class);

  private static final String DEFAULT_JMETER_TEST = "test";

  private Optional<String> env;
  private Optional<String> prop;

  @Inject(optional=true)
  void setEnv(
    @Named("env.JMETER_TEST_JMX")
    final String env
  ) {
    logger.debug("@Inject [env.JMETER_TEST_JMX={}]", env);
    this.env = fromNullable(env);
  };

  @Inject(optional=true)
  void setProp(
    @Named("jmeter.test.jmx")
    final String prop
  ) {
    logger.debug("@Inject [jmeter.test.jmx={}]", prop);
    this.prop = fromNullable(prop);
  };

  @Inject
  JMeterTestProvider(
    final ConfigService configService
  ) {
    // intentionally not using `configService`
    //   - dependency is required for its side-effects.
    this.env = configService.getOptionalEnvVar("env.JMETER_TEST_JMX");
    this.prop = configService.getOptionalAppProp("jmeter.test.jmx");
  }

  @Override
  public String get() {
    logger.debug("env.JMETER_TEST_JMX={}", this.env);
    logger.debug("jmeter.test.jmx={}", this.prop);
    return this.env.or(this.prop).or(DEFAULT_JMETER_TEST);
  }

}
