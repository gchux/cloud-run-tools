package dev.chux.gcp.crun.echo.rest;

import com.google.inject.Inject;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.rest.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class EchoController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(EchoController.class);

  public static final String NAMESPACE = RestModule.NAMESPACE + "/http/request";

  public static final String PROPERTIES_PREFIX = RestModule.PROPERTIES_PREFIX + ".echo.request";
  public static final String PROPERTY_ALLOWED_RUNTIMES = PROPERTIES_PREFIX + ".echo.request";

  @Inject
  public EchoController() {}

  public void register(final String root) {
    path(root, () -> {
      get("/request", "*/*", this);
      post("/request", "*/*", this);
      put("/request", "*/*", this);
      patch("/request", "*/*", this);
      delete("/request", "*/*", this);
      options("/request", "*/*", this);
    });
  }

  public String endpoint(final String root) {
    return "* " + root + "/request";
  }

  public Object handle(final Request request, final Response response) throws Exception {
    logger.info("request: {}", request);
    return request.body();
  }

}
