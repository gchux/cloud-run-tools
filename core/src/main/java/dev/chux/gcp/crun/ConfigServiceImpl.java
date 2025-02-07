package dev.chux.gcp.crun;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.google.common.base.Supplier;

@Singleton
public class ConfigServiceImpl implements ConfigService, Provider<ConfigService>, Supplier<ConfigService> {

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
    return container.getOrDefault(key, defaultValue);
  }

  private final String get(final Map<String, String> container, final String key) {
    return container.get(key);
  }

  @Override
  public String getEnvVar(final String name) {
    return this.get(this.environment, "env." + name);
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
  public String getAppPropOrDefault(final String name, final String defaultValue) {
    return this.getOrDefault(this.properties, name, defaultValue);
  }
  
}
