package dev.chux.gcp.crun.rest;

import java.util.Map;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import spark.Request;
import spark.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

class RestInfoController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RestInfoController.class);

  private final Map<String, Route> routes;

  @Inject
  public RestInfoController(final Map<String, Route> routes) {
    this.routes = routes;
  }

  public void register(final String basePath) {
    path(basePath, () -> {
      get("/routes", this);
    });
  }

  public String endpoint(final String basePath) {
    return "GET " + basePath + "/routes";
  }

  public Object handle(final Request request, final Response response) throws Exception {
    final ServletOutputStream responseOutput = response.raw().getOutputStream();

    for(final Map.Entry<String, Route> entry : this.routes.entrySet()) {
      final Route route = entry.getValue();
      responseOutput.println(entry.getKey() + " => " + route.endpoint(RestAPI.BASE_PATH));
    }
    responseOutput.flush();
    
    return null;
  }

}
