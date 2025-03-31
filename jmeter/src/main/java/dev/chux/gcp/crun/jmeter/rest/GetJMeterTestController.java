package dev.chux.gcp.crun.jmeter.rest;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.jmeter.JMeterTest;
import dev.chux.gcp.crun.jmeter.JMeterTestConfig;
import dev.chux.gcp.crun.jmeter.JMeterTestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GetJMeterTestController extends JMeterTestController {

  private static final Logger logger = LoggerFactory.getLogger(GetJMeterTestController.class);

  private final Gson gson;
  private final JMeterTestService jMeterTestService;

  @Inject
  public GetJMeterTestController(
    final Gson gson,
    final JMeterTestService jMeterTestService
  ) {
    this.gson = gson;
    this.jMeterTestService = jMeterTestService;
  }

  public void register(
    final String basePath
  ) {
    path(basePath, () -> {
      path("/jmeter", () -> {
        path("/test", () -> {
          get("/status", "application/json", this);
          get("/status/:id", "application/json", this);

          head("/status", this);
          head("/status/:id", this);
        });
      });
    });
  }

  public String endpoint(
    final String basePath
  ) {
    return "[GET|HEAD] " + basePath + "/jmeter/test/status/:id";
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    final Optional<String> id = optionalID(request);
    if ( !id.isPresent() ) {
      halt(400, "missing test ID");
      return null;
    }

    final String testID = id.get();

    logger.debug("query: {}", testID);

    final OutputStream stream = response.raw().getOutputStream();

    final Optional<JMeterTest> test = this.jMeterTestService.get(testID);

    if ( !test.isPresent() ) {
      halt(404, "test ID not found: " + testID);
      return null;
    }

    final JMeterTest t = test.get();

    setHeader(response, "id", t.id());
    setHeader(response, "name", t.name());
    setHeader(response, "instance", t.instanceID());
    setHeader(response, "script", t.script());

    final Optional<
      ListenableFuture<JMeterTest>
    > tt = this.jMeterTestService.getTest(testID);

    logger.info("test: {} => {}", test, t);

    if ( tt.isPresent() ) {
      final boolean isDone = tt.get().isDone();
      setHeader(response, "status", isDone ? "complete" : "running");
    }

    if ( isHEAD(request) ) {
      return "";
    }

    response.type("application/json");

    return this.gson.toJson(t, JMeterTestConfig.class);
  }

}
