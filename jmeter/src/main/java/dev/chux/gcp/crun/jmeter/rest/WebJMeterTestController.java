package dev.chux.gcp.crun.jmeter.rest;

import com.google.inject.Inject;

import spark.Request;
import spark.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class WebJMeterTestController extends JMeterTestController {

  private static final Logger logger = LoggerFactory.getLogger(WebJMeterTestController.class);


  @Inject
  public WebJMeterTestController() {}

  @Override
  public void register(
    final String basePath
  ) {
    register(basePath, "web");
    path(apiBase(), () -> {
      get("/web", "text/html", this);
      get("/ui", "text/html", this);
    });
  }

  @Override
  public String endpoint(
    final String basePath
  ) {
    return "[GET] " + apiPath();
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    response.redirect("/ui/index.html");
    return null;
  }

}
