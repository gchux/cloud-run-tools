package dev.chux.gcp.crun.rest;

import com.google.inject.Inject;
import dev.chux.gcp.crun.rest.RestModule.RunJMeterTestRoute;
import spark.Route;

import static spark.Spark.*;

public class RestAPI {

  final Route runJMeterTestRoute;

  @Inject
  RestAPI(@RunJMeterTestRoute Route runJMeterTestRoute) {
    this.runJMeterTestRoute = runJMeterTestRoute;
  }
  
  public void serve(final int httpPort) {
    port(httpPort);
    path("/jmeter", () -> {
      path("/test", () -> {
        get("/run", this.runJMeterTestRoute);
      });
    });
  }

}
