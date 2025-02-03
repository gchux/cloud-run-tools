package dev.chux.gcp.crun;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap;

import dev.chux.gcp.crun.rest.RestModule;
import dev.chux.gcp.crun.process.ProcessModule;
import dev.chux.gcp.crun.jmeter.JMeterModule;
import dev.chux.gcp.crun.http.HttpModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(AppModule.class);

  private static final String PROPERTIES_FILE = "/jmeter-test-runner.properties";

  private static final String SERVER_PORT_ENV = "PORT";
  private static final String SERVER_PORT_PROP = "server.port";

  protected void configure() {
    Names.bindProperties(binder(), loadEnvironment());
    Names.bindProperties(binder(), loadProperties(PROPERTIES_FILE));

    install(new ProcessModule());
    install(new RestModule());
    install(new JMeterModule());
    install(new HttpModule());
  }

  private final Map<String, String> loadEnvironment() {
    final Map<String, String> environment = Maps.newHashMap(System.getenv());

    final ImmutableMap.Builder<String, String> env = ImmutableMap.builderWithExpectedSize(environment.size());
    for (String key : environment.keySet()) {
      env.put("env." + key, environment.get(key));
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

    final Map<String, String> propertiesMap = Maps.fromProperties(properties);

    logger.info("properties: {}", propertiesMap);

    return ImmutableMap.copyOf(propertiesMap);
  }

  private static final void ensureKey(final Map<String, String> map,
    final String key, final String defaultValue) {
    if (!map.containsKey(key)) {
      map.put(key, defaultValue);
    }
  }  

}
