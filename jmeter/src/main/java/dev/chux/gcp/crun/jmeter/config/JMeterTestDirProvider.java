package dev.chux.gcp.crun.jmeter.config;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import com.google.common.base.Supplier;
import com.google.common.base.Optional;

import static com.google.common.base.Optional.fromNullable;

public class JMeterTestDirProvider implements Provider<String>, Supplier<String> {

  private static final String DEFAULT_JMETER_JMX_DIR = "/jmx";

  @Inject(optional=true)
  @Named("env.JMETER_JMX_DIR")
  String jmxDirEnv = null;

  @Inject(optional=true)
  @Named("jmeter.jmx.dir")
  String jmxDirProp = null;

  @Override
  public String get() {
    return optionalEnv().or(optionalProp()).or(DEFAULT_JMETER_JMX_DIR);
  }

  private final Optional<String> optionalEnv() {
    return fromNullable(this.jmxDirEnv);
  }

  private final Optional<String> optionalProp() {
    return fromNullable(this.jmxDirProp);
  }

}
