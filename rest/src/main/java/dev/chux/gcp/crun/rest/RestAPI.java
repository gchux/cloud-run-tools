package dev.chux.gcp.crun.rest;

import java.util.Map;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class RestAPI {
  private static final Logger logger = LoggerFactory.getLogger(RestAPI.class);

  private static final String BASE_PATH = "/x";

  private final Map<String, Route> routes;

  @Inject
  RestAPI(final Map<String, Route> routes) {
    this.routes = routes;
  }
  
  public void serve(final int httpPort) {
    port(httpPort);
    registerRoutes();
  }

  private final void registerRoutes() {
    for(final Map.Entry<String, Route> entry : this.routes.entrySet()) {
      final Route route = entry.getValue();
      logger.info("registering route '{}' for: {}", entry.getKey(), route.endpoint(BASE_PATH));
      route.register(BASE_PATH);
    }
  }

}
