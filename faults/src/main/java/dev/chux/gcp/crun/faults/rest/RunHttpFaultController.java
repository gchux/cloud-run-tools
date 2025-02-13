package dev.chux.gcp.crun.faults.rest;

import java.io.OutputStream;
import java.util.UUID;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.faults.FaultsService;
import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.rest.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class RunHttpFaultController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunHttpFaultController.class);

  private static final String SYS_OUT = "sys";

  private final Gson gson;
  private final FaultsService faultsService;

  @Inject
  public RunHttpFaultController(
    final Gson gson,
    final FaultsService faultsService
  ) {
    this.gson = gson;
    this.faultsService = faultsService;
  }

  public void register(final String basePath) {
    path(basePath, () -> {
      path("/faults", () -> {
        path("/http", () -> {
          post("/", "*/*", this);
          post("/:runtime", "*/*", this); 
        });
      });
    });
  }

  public String endpoint(final String basePath) {
    return "POST " + basePath + "/faults/http/:runtime/:fault";
  }

  public Object handle(final Request request, final Response response) throws Exception {
  final String executionID = UUID.randomUUID().toString();

    final String rawBody = request.body();
    final Optional<HttpRequest> httpRequest = this.httpRequest(rawBody);
    if (!httpRequest.isPresent()) {
      halt(404, "invalid HTTP request");
      return null;
    }

    final String runtime = emptyToNull(request.params(":runtime"));

    logger.info("starting: {}", executionID);

    logger.info("runtime: {} | HTTP request: {}", runtime, httpRequest.get());

    final Optional<OutputStream> output = Optional.of(response.raw().getOutputStream());

    this.faultsService.runHttpRequest(httpRequest.get(), fromNullable(runtime), output, output);

    logger.info("finished: {}", executionID);

    return httpRequest.get().toString();
  }

  private final Optional<HttpRequest> httpRequest(final String rawBody) {
    try {
      final HttpRequest request = this.gson.fromJson(rawBody, HttpRequest.class);
      return fromNullable(request);
    } catch(Exception ex) {
      logger.error("invalid HTTP request: {}", rawBody);
      logger.error("failed to parse json", getStackTraceAsString(ex));
    }
    return absent();
  }

}
