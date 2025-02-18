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
import dev.chux.gcp.crun.model.GoogleAPIsHttpRequest;
import dev.chux.gcp.crun.rest.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class RunGoogleAPIsFaultController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunGoogleAPIsFaultController.class);

  public static final String PROPERTIES_PREFIX = RestModule.PROPERTIES_PREFIX + ".googleapis.http.request";
  public static final String PROPERTY_ALLOWED_RUNTIMES = PROPERTIES_PREFIX + ".runtimes.allowed";

  private static final String SYS_OUT = "sys";

  public static final String KEY = RestModule.NAMESPACE + "/googleapis/http/request";

  private final Gson gson;
  private final FaultsService faultsService;
  private final Set<String> allowedRuntimes;

  @Inject
  public RunGoogleAPIsFaultController(
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
      path("/googleapis", () -> {
        path("/http", () -> {
          post("/:runtime", "*/*", this); 
        });

        path("/curl", () -> {
          post("/:runtime", "*/*", this); 
        });
      });

      path("/gapis", () -> {
        path("/http", () -> {
          post("/:runtime", "*/*", this); 
        });

        path("/curl", () -> {
          post("/:runtime", "*/*", this); 
        });
      });
    });
    logger.info("allowed runtimes: {}", this.allowedRuntimes);
  }

  public String endpoint(final String root) {
    return "POST " + root + "/(googleapis|gapis)/(http|curl)/:runtime";
  }

  public Object handle(final Request request, final Response response) throws Exception {
    final String executionID = UUID.randomUUID().toString();

    final String rawBody = request.body();
    final Optional<GoogleAPIsHttpRequest> grequest = this.gapisRequest(rawBody);

    if (!grequest.isPresent()) {
      halt(400, "invalid HTTP request");
      return null;
    }

    final Optional<String> runtime = this.runtime(request);

    if (!runtime.isPresent()) {
      halt(400, "missing runtime");
      return null;
    }

    if (!this.isAllowedRuntime(runtime)) {
      halt(404, "invalid runtime: " + runtime.get());
      return null;
    }

    response.header("x-faults-execution-id", executionID);

    logger.info("starting: {}", executionID);

    logger.info("runtime: {} | Google APIs request: {}", runtime, grequest.get());

    final Optional<OutputStream> output = Optional.of(response.raw().getOutputStream());

    this.faultsService.runGoogleAPIsHttpRequest(grequest.get(), runtime.get(), output, output);

    logger.info("finished: {}", executionID);

    return grequest.get().toString();
  }

  private final boolean isAllowedRuntime(final Optional<String> runtime) {
    return !runtime.isPresent() || this.allowedRuntimes.contains(runtime.get());
  }

  private final Optional<String> runtime(final Request request) {
    return fromNullable(emptyToNull(request.params(":runtime")));
  }

  private final Optional<GoogleAPIsHttpRequest> gapisRequest(final String rawBody) {
    try {
      final GoogleAPIsHttpRequest request = this.gson.fromJson(rawBody, GoogleAPIsHttpRequest.class);
      return fromNullable(request);
    } catch(Exception ex) {
      logger.error("invalid Google APIs request: {}", rawBody);
      logger.error("failed to parse json", getStackTraceAsString(ex));
    }
    return absent();
  }

}
