package dev.chux.gcp.crun;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;

import com.google.inject.Module;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.Provides;
import com.google.inject.name.Names;

import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.configuration.SystemConfigurationProvider;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.configuration.CompositeConfigurationProvider;


import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import dev.chux.gcp.crun.process.ProcessModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

class AppModule extends AbstractModule implements BootstrapModule {
  private static final Logger logger = LoggerFactory.getLogger(AppModule.class);

  private static final TypeLiteral<Map<String, String>> MapTypeStringString = new TypeLiteral<Map<String, String>>() {};

  private final String propertiesFile;
  private final Optional<Module> module;

  private final Map<String, String> environmentMap;
  private final Map<String, String> propertiesMap;

  AppModule(@NonNull @CheckForNull final String propertiesFile, @CheckForNull Optional<Module> module) {
    checkArgument(!isNullOrEmpty(propertiesFile), "full path to properties file is required");
    this.propertiesFile = propertiesFile;
    this.module = checkNotNull(module, "optional module is required");

    this.environmentMap = loadEnvironment();
    this.propertiesMap = loadProperties(this.propertiesFile);
  }

  private static final String SERVER_PORT_ENV = "PORT";
  private static final String SERVER_PORT_PROP = "server.port";

  public void configure(final BootstrapBinder binder) {
    final PropertiesConfigurationProvider environmentProvider =
      newPropertiesProvider(this.environmentMap, absent());
    final PropertiesConfigurationProvider propertiesProvider =
      newPropertiesProvider(this.propertiesMap, Optional.of(this.environmentMap));
    final SystemConfigurationProvider systemPropertiesProvider = new SystemConfigurationProvider(this.environmentMap);

    final CompositeConfigurationProvider compositeConfigProvider =
      new CompositeConfigurationProvider(systemPropertiesProvider, environmentProvider, propertiesProvider);

    binder.bindConfigurationProvider().toInstance(compositeConfigProvider);
  }

  protected void configure() {
    Names.bindProperties(binder(), this.environmentMap);
    bindConfiguration("app://environment", this.environmentMap);

    Names.bindProperties(binder(), this.propertiesMap);
    bindConfiguration("app://properties", this.propertiesMap);

    bind(ConfigService.class).to(ConfigServiceImpl.class).asEagerSingleton();

    install(new ProcessModule());

    if (this.module.isPresent()) {
      install(this.module.get());
    }
  }

  private final PropertiesConfigurationProvider newPropertiesProvider(
    final Map<String, String> map, final Optional<Map<String, String>> variables) {
    final Properties properties = new Properties();
    properties.putAll(map);

    if (variables.isPresent()) {
      return new PropertiesConfigurationProvider(properties, variables.get());
    }
    return new PropertiesConfigurationProvider(properties);
  }

  private final void bindConfiguration(final String key, final Map<String, String> config) {
    bind(MapTypeStringString).annotatedWith(Names.named(key)).toInstance(config);
  }

  private final Map<String, String> loadEnvironment() {
    final Map<String, String> environment = Maps.<String, String>newHashMap(System.getenv());

    final ImmutableMap.Builder<String, String> env = ImmutableMap.<String, String>builderWithExpectedSize(environment.size());
    for (Map.Entry<String, String> entry : environment.entrySet()) {
      env.put("env." + entry.getKey(), entry.getValue());
    }

    final Map<String, String> environmentMap = env.build();

    logger.info("environment: {}", environmentMap);

    return environmentMap;
  }

  private final Map<String,String> loadProperties(final String propertiesFile) {
    final Properties properties = new Properties();

    try (final InputStream input = new FileInputStream(propertiesFile)) {
      properties.load(input);
    } catch(Exception e) {
      e.printStackTrace(System.err);
    }

    if (properties.isEmpty()) {
      return ImmutableMap.of();
    }

    Map<String, String> propertiesMap = Maps.<String, String>fromProperties(properties);
    propertiesMap = ImmutableMap.<String, String>copyOf(propertiesMap);

    logger.info("properties: {}", propertiesMap);

    return propertiesMap;
  }

  private static final void ensureKey(final Map<String, String> map,
    final String key, final String defaultValue) {
    if (!map.containsKey(key)) {
      map.put(key, defaultValue);
    }
  }  

}
