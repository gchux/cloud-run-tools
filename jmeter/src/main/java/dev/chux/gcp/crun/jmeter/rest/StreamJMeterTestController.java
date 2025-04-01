package dev.chux.gcp.crun.jmeter.rest;

import javax.servlet.ServletOutputStream;

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

  @Override
  public void register(
    final String basePath
  ) {
    super.register(basePath, "stream");
    path(apiBase(), () -> {
      get("/stream", "text/plain", this);
      get("/stream/:id", "text/plain", this);
    });
  }

  @Override
  public String endpoint(
    final String basePath
  ) {
    return "[GET] " + apiPath() + "/:id";
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

    setHeader(response, "id", testID);

    response.type("text/plain");

    final ServletOutputStream stream = response.raw().getOutputStream();

    final Optional<JMeterTest> test = this.jMeterTestService.get(testID);

    if ( !test.isPresent() ) {
      halt(404, "test ID not found: " + testID);
      return null;
    }
    
    JMeterTest t = test.get();

    logger.info("connecting to test: {}", t);

    stream.println("---- stream/start: <" + t.id() + "> ----");

    final ListenableFuture<
      JMeterTest
    > futureTest = this.jMeterTestService
      .connect(t, stream)
      .or(Futures.immediateFuture(t));

    // block until test is complete
    t = futureTest.get();

    stream.println("---- stream/stop: <" + t.id() + "> ----");
    stream.flush();

    return null;
  }

}
