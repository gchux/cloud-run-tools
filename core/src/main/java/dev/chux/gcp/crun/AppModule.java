package dev.chux.gcp.crun;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.inject.Module;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import dev.chux.gcp.crun.rest.RestModule;
import dev.chux.gcp.crun.process.ProcessModule;
import dev.chux.gcp.crun.http.HttpModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(AppModule.class);

  private final String propertiesFile;
  private final Optional<Module> module;

  AppModule(@NonNull @CheckForNull final String propertiesFile, @Nullable Module module) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(propertiesFile), "full path to properties file is required");
    this.propertiesFile = propertiesFile;
    this.module = Optional.fromNullable(module);
  }

  private static final String SERVER_PORT_ENV = "PORT";
  private static final String SERVER_PORT_PROP = "server.port";

  protected void configure() {
    Names.bindProperties(binder(), loadEnvironment());
    Names.bindProperties(binder(), loadProperties(this.propertiesFile));

    install(new ProcessModule());
    install(new RestModule());
    install(new HttpModule());

    if (this.module.isPresent()) {
      install(this.module.get());
    }
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

    final Map<String, String> propertiesMap = Maps.<String, String>fromProperties(properties);

    logger.info("properties: {}", propertiesMap);

    return ImmutableMap.<String, String>copyOf(propertiesMap);
  }

  private static final void ensureKey(final Map<String, String> map,
    final String key, final String defaultValue) {
    if (!map.containsKey(key)) {
      map.put(key, defaultValue);
    }
  }  

}
