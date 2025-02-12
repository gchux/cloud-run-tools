package dev.chux.gcp.crun.faults.rest;

import java.util.UUID;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.rest.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;
import static com.google.common.base.Strings.isNullOrEmpty;

public class RunHttpFaultController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunHttpFaultController.class);

  private static final String SYS_OUT = "sys";

  private final Gson gson;

  @Inject
  public RunHttpFaultController(final Gson gson) {
    this.gson = gson;
  }

  public void register(final String basePath) {
    path(basePath, () -> {
      path("/faults", () -> {
        path("/http", () -> {
          path("/:runtime", () -> {
            post("/:fault", "*/*", this); 
          });
        });
      });
    });
  }

  public String endpoint(final String basePath) {
    return "POST " + basePath + "/faults/http/:runtime/:fault";
  }

  public Object handle(final Request request, final Response response) throws Exception {
    final String executionID = UUID.randomUUID().toString();

    final String runtime = request.params(":runtime");
    final String fault = request.params(":fault");
    
    logger.info("starting: {}", executionID);

    logger.info("runtime: {} | fault: {}", runtime, fault);

    logger.info("finished: {}", executionID);

    return null;
  }

}
