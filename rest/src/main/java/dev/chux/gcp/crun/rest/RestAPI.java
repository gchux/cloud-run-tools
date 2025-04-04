package dev.chux.gcp.crun.rest;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;

import dev.chux.gcp.crun.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class RestAPI implements Supplier<Map<String, Route>> {

  private static final Logger logger = LoggerFactory.getLogger(RestAPI.class);

  private static final String PROPERTY_REST_ROOT = "rest.root";
  private static final String DEFAULT_ROOT = "";

  private final Optional<String> root;
  private final Map<String, Route> routes;

  @Inject
  RestAPI(
    final ConfigService configService,
    final Map<String, Route> routes
  ) {
    this.routes = routes;
    this.root = root(configService);
  }

  static final Optional<String> root(final ConfigService configService) {
    return configService.getOptionalAppProp(PROPERTY_REST_ROOT);
  }

  public final String root() {
    return this.root.or(DEFAULT_ROOT);
  }

  public Map<String, Route> get() {
    return this.routes;
  }
  
  public void serve(final int httpPort) {
    port(httpPort);
    staticFiles.location("/static");
    staticFiles.header("Content-Encoding", "gzip");
    registerRoutes();
  }

  private final void registerRoutes() {
    for(final Map.Entry<String, Route> entry : this.routes.entrySet()) {
      final Route route = entry.getValue();
      final String root = this.root();
      route.register(root);
      logger.info("registered route '{}' for: {}", entry.getKey(), route.endpoint(root));
    }
  }

}
