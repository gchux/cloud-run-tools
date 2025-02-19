package dev.chux.gcp.crun.faults.rest;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

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
import dev.chux.gcp.crun.model.HttpRequests;
import dev.chux.gcp.crun.rest.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

import static java.nio.charset.StandardCharsets.UTF_8;

import static java.util.Collections.singletonList;

public class RunHttpFaultController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunHttpFaultController.class);

  public static final String PROPERTIES_PREFIX = RestModule.PROPERTIES_PREFIX + ".http.request";
  public static final String PROPERTY_ALLOWED_RUNTIMES = PROPERTIES_PREFIX + ".runtimes.allowed";

  private static final String SYS_OUT = "sys";

  public static final String KEY = RestModule.NAMESPACE + "/http/request";

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
      post("/http", "*/*", this);
      path("/http", () -> {
        post("/", "*/*", this);
        post("/:runtime", "*/*", this); 
      });

      post("/curl", "*/*", this);
      path("/curl", () -> {
        post("/", "*/*", this);
        post("/:runtime", "*/*", this); 
      });
    });
    logger.info("allowed runtimes: {}", this.allowedRuntimes);
  }

  public String endpoint(final String root) {
    return "POST " + root + "/(http|curl)[/[optional:fault]]";
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    final String executionID = UUID.randomUUID().toString();

    final String rawBody = request.body();

    final Optional<HttpRequests> httpRequests;
    if (isMultiple(request, "type")) {
      httpRequests = this.requestsPayload(rawBody);
    } else {
      httpRequests = this.requestPayload(rawBody);
    }

    if (!httpRequests.isPresent()) {
      logger.error("invalid payload: {}", rawBody);
      halt(400, "invalid HTTP request(s)");
      return null;
    }

    final Optional<String> runtime = this.runtime(request);
    if (!this.isAllowedRuntime(runtime)) {
      halt(404, "invalid runtime: " + runtime.get());
      return null;
    }

    response.header("x-execution-id", executionID);

    logger.info("starting: {}", executionID);

    this.execHttpRequests(request, response, runtime, httpRequests.get());

    logger.info("finished: {}", executionID);

    return httpRequests.get().toString();
  }

  private final void execHttpRequest(
    final Request request,
    final Response response,
    final Optional<String> runtime,
    final HttpRequest httpRequest,
    final OutputStream stream
  ) {
    logger.info("runtime: {} | HTTP request: {}", runtime, httpRequest);
    final Optional<OutputStream> output = Optional.of(stream);
    this.faultsService.runHttpRequest(httpRequest, runtime, output, output);
  }

  private final void execHttpRequests(
    final Request request,
    final Response response,
    final Optional<String> runtime,
    final HttpRequests httpRequests
  ) throws Exception {
    final List<HttpRequest> tasks = httpRequests.get();

    if (tasks.isEmpty()) {
      halt(204, "no HTTP requests");
      return;
    }

    final OutputStream output = response.raw().getOutputStream();
    final OutputStreamWriter writer = new OutputStreamWriter(output, UTF_8);

    for(final HttpRequest httpRequest : tasks) {
      this.execHttpRequest(request, response, runtime, httpRequest, output);
      writer.write("\n---\n");
      writer.flush();
    }
  }

  private final Optional<String> runtime(final Request request) {
    return fromNullable(emptyToNull(request.params(":runtime")));
  }

  private final Optional<HttpRequests> toHttpRequests(
    final Optional<HttpRequest> request
  ) {
    if (!request.isPresent()) {
      return absent();
    }
    return Optional.of(
      this.newHttpRequests(
        singletonList(request.get())
      )
    );
  }

  private final HttpRequests newHttpRequests(
    final List<HttpRequest> requests
  ) {
    return new HttpRequests(requests);
  }

  private final boolean isAllowedRuntime(final Optional<String> runtime) {
    return !runtime.isPresent() || this.allowedRuntimes.contains(runtime.get());
  }

  private final Optional<HttpRequests> requestPayload(final String rawJSON) {
    // see: https://github.com/gchux/jmeter-test-runner/blob/main/model/src/main/java/dev/chux/gcp/crun/model/HttpRequest.java
    try {
      final Optional<HttpRequest> httpRequest = jsonPayload(this.gson, rawJSON, HttpRequest.class);
      return this.toHttpRequests(httpRequest);
    } catch(final Exception ex) {
      logger.error("failed to parse HTTP request", getStackTraceAsString(ex));
    }
    return absent();
  }

  private final Optional<HttpRequests> requestsPayload(final String rawJSON) {
    // see: https://github.com/gchux/jmeter-test-runner/blob/main/model/src/main/java/dev/chux/gcp/crun/model/HttpRequests.java
    try {
      return jsonPayload(this.gson, rawJSON, HttpRequests.class);
    } catch(final Exception ex) {
      logger.error("failed to parse json", getStackTraceAsString(ex));
    }
    return absent();
  }

}
