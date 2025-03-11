package dev.chux.gcp.crun.jmeter.rest;

import java.util.UUID;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.rest.Route;
import dev.chux.gcp.crun.jmeter.JMeterTestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.fromNullable;
import static spark.Spark.*;

public class RunJMeterTestController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunJMeterTestController.class);

  private static final String SYS_OUT = "sys";

  private final JMeterTestService jMeterTestService;

  @Inject
  public RunJMeterTestController(final JMeterTestService jMeterTestService) {
    this.jMeterTestService = jMeterTestService;
  }

  public void register(final String basePath) {
    path(basePath, () -> {
      path("/jmeter", () -> {
        path("/test", () -> {
          get("/run", this);
        });
      });
    });
  }

  public String endpoint(final String basePath) {
    return "GET " + basePath + "/jmeter/test/run";
  }

  public Object handle(final Request request, final Response response) throws Exception {
    final String testId = UUID.randomUUID().toString();
    final String output = request.queryParams("output"); 
    final ServletOutputStream responseOutput = response.raw().getOutputStream();

    response.type("text/plain");
    response.header("x-jmeter-test", testId);

    final String host = request.queryParamOrDefault("host", null);
    if (Strings.isNullOrEmpty(host)) {
      response.status(400);
      return "host is required";
    }

    final Optional<String> jmx = fromNullable(request.queryParamOrDefault("jmx", null));
    final Optional<String> proto = fromNullable(request.queryParamOrDefault("proto", null));
    final Optional<String> method = fromNullable(request.queryParamOrDefault("method", null));
    final Optional<String> path = fromNullable(request.queryParamOrDefault("path", null));
    final Optional<Integer> port = fromNullable(Ints.tryParse(request.queryParamOrDefault("port", ""), 10));

    final int concurrency = Integer.parseInt(request.queryParamOrDefault("concurrency", "1"), 10);
    final int duration = Integer.parseInt(request.queryParamOrDefault("duration", "1"), 10);
    final int rampupTime = Integer.parseInt(request.queryParamOrDefault("rampup_time", "1"), 10);
    final int rampupSteps = Integer.parseInt(request.queryParamOrDefault("rampup_steps", "1"), 10);

    responseOutput.println("---- starting: " + testId + " ----");
    logger.info("starting: {}", testId);

    if( output != null && output.equalsIgnoreCase(SYS_OUT) ) {
      this.jMeterTestService.start(testId,
        jmx, proto, method, host, port, path,
        concurrency, duration, rampupTime, rampupSteps);
    } else {
      this.jMeterTestService.start(testId,
        jmx, proto, method, host, port, path,
        concurrency, duration, rampupTime, rampupSteps,
        responseOutput, false /* closeable */);
    }
    
    logger.info("finished: {}", testId);
    return "---- finished: " + testId + " ----";
  }

}
