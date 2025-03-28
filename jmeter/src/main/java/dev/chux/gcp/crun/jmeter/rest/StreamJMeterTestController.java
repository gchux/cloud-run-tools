package dev.chux.gcp.crun.jmeter.rest;

import java.io.OutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.jmeter.JMeterTest;
import dev.chux.gcp.crun.jmeter.JMeterTestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class StreamJMeterTestController extends JMeterTestController {

  private static final Logger logger = LoggerFactory.getLogger(StreamJMeterTestController.class);

  private final JMeterTestService jMeterTestService;

  @Inject
  public StreamJMeterTestController(
    final JMeterTestService jMeterTestService
  ) {
    this.jMeterTestService = jMeterTestService;
  }

  public void register(
    final String basePath
  ) {
    path(basePath, () -> {
      path("/jmeter", () -> {
        path("/test", () -> {
          get("/stream", this);
        });
      });
    });
  }

  public String endpoint(
    final String basePath
  ) {
    return "[GET] " + basePath + "/jmeter/test/stream";
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

    this.setHeader(response, "id", testID);

    response.type("text/plain");

    final OutputStream stream = response.raw().getOutputStream();

    final Optional<JMeterTest> test = this.jMeterTestService.get(testID);
    if ( test.isPresent() ) {
      JMeterTest t = test.get();
      
      logger.info("connecting to test: {}", t);

      final ListenableFuture<
        JMeterTest
      > futureTest = this.jMeterTestService
        .connect(t, stream)
        .or(Futures.immediateFuture(t));

      stream.flush();
      t = futureTest.get();
    } else {
      halt(404, "test ID not found: " + testID);
    }
    return testID;
  }

}
