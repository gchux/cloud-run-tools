package dev.chux.gcp.crun.faults.rest;

import java.io.OutputStream;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.ConfigService;
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

  public static final String PROPERTIES_PREFIX = RestModule.PROPERTIES_PREFIX + ".http.request";
  public static final String PROPERTY_ALLOWED_RUNTIMES = PROPERTIES_PREFIX + ".runtimes.allowed";

  private static final String SYS_OUT = "sys";

  public static final String NAMESPACE = RestModule.NAMESPACE + "/http/request";

  private final Gson gson;
  private final FaultsService faultsService;
  private final Set<String> allowedRuntimes;

  @Inject
  public RunHttpFaultController(
    final ConfigService configService,
    final Gson gson,
    final FaultsService faultsService
  ) {
    this.gson = gson;
    this.faultsService = faultsService;
    this.allowedRuntimes = this.allowedRuntimes(configService);
  }

  private final Set<String> allowedRuntimes(final ConfigService configService) {
    return ImmutableSet.copyOf(allowedRuntimesProperty(configService));
  }

  private final List<String> allowedRuntimesProperty(final ConfigService configService) {
    return configService.getMultivalueAppProp(PROPERTY_ALLOWED_RUNTIMES);
  }

  public void register(final String root) {
    path(root, () -> {
      path("/http", () -> {
        post("/", "*/*", this);
        post("/:runtime", "*/*", this); 
      });
    });
    logger.info("allowed runtimes: {}", this.allowedRuntimes);
  }

  public String endpoint(final String root) {
    return "POST " + root + "/http/[optional:fault]";
  }

  public Object handle(final Request request, final Response response) throws Exception {
    final String executionID = UUID.randomUUID().toString();

    final String rawBody = request.body();
    final Optional<HttpRequest> httpRequest = this.httpRequest(rawBody);
    if (!httpRequest.isPresent()) {
      halt(404, "invalid HTTP request");
      return null;
    }

    final Optional<String> runtime = this.runtime(request);
    if (!this.isAllowedRuntime(runtime)) {
      halt(404, "invalid runtime: " + runtime.get());
      return null;
    }

    logger.info("starting: {}", executionID);

    logger.info("runtime: {} | HTTP request: {}", runtime, httpRequest.get());

    final Optional<OutputStream> output = Optional.of(response.raw().getOutputStream());

    this.faultsService.runHttpRequest(httpRequest.get(), runtime, output, output);

    logger.info("finished: {}", executionID);

    return httpRequest.get().toString();
  }

  private final boolean isAllowedRuntime(final Optional<String> runtime) {
    return !runtime.isPresent() || this.allowedRuntimes.contains(runtime.get());
  }

  private final Optional<String> runtime(final Request request) {
    return fromNullable(emptyToNull(request.params(":runtime")));
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
