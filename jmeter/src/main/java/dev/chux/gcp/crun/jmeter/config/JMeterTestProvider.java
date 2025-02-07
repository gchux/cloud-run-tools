package dev.chux.gcp.crun.jmeter.config;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import com.google.common.base.Supplier;
import com.google.common.base.Optional;

import static com.google.common.base.Optional.fromNullable;

public class JMeterTestProvider implements Provider<String>, Supplier<String> {

  private static final String DEFAULT_JMETER_TEST = "test";

  @Inject(optional=true)
  @Named("env.JMETER_TEST_JMX")
  String jmxEnv = null;

  @Inject(optional=true)
  @Named("jmeter.test.jmx")
  String jmxProp = null;

  public String get() {
    return optionalEnv().or(optionalProp()).or(DEFAULT_JMETER_TEST);
  }

  private final Optional<String> optionalEnv() {
    return fromNullable(this.jmxEnv);
  }

  private final Optional<String> optionalProp() {
    return fromNullable(this.jmxProp);
  }

}
