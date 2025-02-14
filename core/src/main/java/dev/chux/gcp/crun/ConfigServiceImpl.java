package dev.chux.gcp.crun;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Doubles;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@Singleton
public class ConfigServiceImpl implements ConfigService, Provider<ConfigService>, Supplier<ConfigService> {

  private static final Splitter multivalued = Splitter.on(',').trimResults().omitEmptyStrings();

  private final Map<String, String> environment;
  private final Map<String, String> properties;

  @Inject
  ConfigServiceImpl(
    @Named("app://environment") Map<String, String> environment,
    @Named("app://properties") Map<String, String> properties
  ) {
    this.environment = environment;
    this.properties = properties;
  }

  @Override
  public ConfigService get() {
    return this;
  }

  private final String getOrDefault(final Map<String, String> container, final String key, final String defaultValue) {
    return emptyToNull(container.getOrDefault(key, defaultValue));
  }

  private final String get(final Map<String, String> container, final String key) {
    return emptyToNull(container.get(key));
  }

  private final List<String> getMultivalued(final Map<String, String> container, final String key) {
    final String value = getOrDefault(container, key, "");
    if (isNullOrEmpty(value)) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(multivalued.splitToList(value));
  }

  private final Optional<Integer> parseIntValue(final String value) {
    if (isNullOrEmpty(value)) {
      return absent();
    }
    return fromNullable(Ints.tryParse(value, 10));
  }

  private final Optional<Long> parseLongValue(final String value) {
    if (isNullOrEmpty(value)) {
      return absent();
    }
    return fromNullable(Longs.tryParse(value, 10));
  }

  private final Optional<Double> parseDoubleValue(final String value) {
    if (isNullOrEmpty(value)) {
      return absent();
    }
    return fromNullable(Doubles.tryParse(value));
  }

  private final Optional<Boolean> parseBooleanValue(final String value) {
    if (isNullOrEmpty(value)) {
      return absent();
    }
    return Optional.of(Boolean.parseBoolean(value));
  }

  @Override
  public String getEnvVar(final String name) {
    return this.get(this.environment, "env." + name);
  }

  @Override
  public List<String> getMultivalueEnvVar(final String name) {
    return this.getMultivalued(this.environment, "env." + name);
  }

  @Override
  public Optional<Integer> getIntEnvVar(final String name) {
    return this.parseIntValue(this.getEnvVar(name));
  }

  @Override
  public Optional<Long> getLongEnvVar(final String name) {
    return this.parseLongValue(this.getEnvVar(name));
  }

  @Override
  public Optional<Double> getDoubleEnvVar(final String name) {
    return this.parseDoubleValue(this.getEnvVar(name));
  }

  @Override
  public Optional<Boolean> getBooleanEnvVar(final String name) {
    return this.parseBooleanValue(this.getEnvVar(name));
  }

  @Override
  public Optional<String> getOptionalEnvVar(final String name) {
    return fromNullable(this.getEnvVar(name));
  }

  @Override
  public String getEnvVarOrDefault(final String name, final String defaultValue) {
    return this.getOrDefault(this.environment, "env." + name, defaultValue);
  }

  @Override
  public String getAppProp(final String name) {
    return this.get(this.properties, name);
  }

  @Override
  public List<String> getMultivalueAppProp(final String name) {
    return this.getMultivalued(this.properties, name);
  }

  @Override
  public Optional<Integer> getIntAppProp(final String name) {
    return this.parseIntValue(this.getAppProp(name));
  }

  @Override
  public Optional<Long> getLongAppProp(final String name) {
    return this.parseLongValue(this.getAppProp(name));
  }

  @Override
  public Optional<Double> getDoubleAppProp(final String name) {
    return this.parseDoubleValue(this.getAppProp(name));
  }

  @Override
  public Optional<Boolean> getBooleanAppProp(final String name) {
    return this.parseBooleanValue(this.getAppProp(name));
  }

  @Override
  public Optional<String> getOptionalAppProp(final String name) {
    return fromNullable(this.getAppProp(name));
  }

  @Override
  public String getAppPropOrDefault(final String name, final String defaultValue) {
    return this.getOrDefault(this.properties, name, defaultValue);
  }
  
}
