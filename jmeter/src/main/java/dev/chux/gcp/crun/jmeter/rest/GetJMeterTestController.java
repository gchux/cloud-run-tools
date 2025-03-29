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
        });
      });
    });
  }

  public String endpoint(
    final String basePath
  ) {
    return "[GET] " + basePath + "/jmeter/test/status/:id";
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

    setHeader(response, "id", testID);

    response.type("application/json");

    final OutputStream stream = response.raw().getOutputStream();

    final Optional<JMeterTest> test = this.jMeterTestService.get(testID);

    if ( !test.isPresent() ) {
      halt(404, "test ID not found: " + testID);
      return null;
    }

    final Optional<
      ListenableFuture<JMeterTest>
    > t = this.jMeterTestService.getTest(testID);

    logger.info("test: {} => {}", test, t);

    if ( t.isPresent() ) {
      final boolean isDone = t.get().isDone();
      setHeader(response, "status", isDone ? "complete" : "running");
    }

    final OutputStreamWriter writer = new OutputStreamWriter(stream, UTF_8);

    this.gson.toJson(test.get().get(), JMeterTestConfig.class, writer);

    writer.flush();

    return null;
  }

}
