package dev.chux.gcp.crun.jmeter.rest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.rest.Route;
import dev.chux.gcp.crun.jmeter.JMeterTestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.isNullOrEmpty;

import static spark.Spark.*;

public class RunJMeterTestController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunJMeterTestController.class);

  public static final String PROPERTY_JMETER_MODES = "jmeter.modes";

  private static final String SYS_OUT = "sys";

  private final JMeterTestService jMeterTestService;
  private final Set<String> modes;

  @Inject
  public RunJMeterTestController(
    final ConfigService configService,
    final JMeterTestService jMeterTestService
  ) {
    this.jMeterTestService = jMeterTestService;
    this.modes = this.jmeterModes(configService);
  }

  private Set<String> jmeterModes(
    final ConfigService configService
  ) {
    return ImmutableSet.copyOf(jmeterModesProperty(configService));
  }
  
  private final List<String> jmeterModesProperty(
    final ConfigService configService
  ) {
    return configService.getMultivalueAppProp(PROPERTY_JMETER_MODES);
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
    final String output = request.queryParamOrDefault("output", "res"); 
    final ServletOutputStream responseOutput = response.raw().getOutputStream();

    response.type("text/plain");
    response.header("x-jmeter-test", testId);

    // FQDN, hostname or IP of the remote service.
    final String host = request.queryParamOrDefault("host", null);
    if ( isNullOrEmpty(host) ) {
      response.status(400);
      return "host is required";
    }

    // Operation mode of the Load Test, may be: `qps` or `concurrency`.
    final String mode = request.queryParamOrDefault("mode", "concurrency").toLowerCase();

    if ( isNullOrEmpty(mode) ) {
      halt(400, "mode is required");
      return null;
    }

    if ( !this.modes.contains(mode) ) {
      halt(404, "invalid mode: " + mode);
      return null;
    }

    // test to execute base on the name of JMX files ( case sensitive ).
    final Optional<String> jmx     = fromNullable(request.queryParamOrDefault("jmx", null));
    //
    // may be `http` ot `https` ( case insensitive ).
    final Optional<String> proto   = fromNullable(request.queryParamOrDefault("proto", null));

    // HTTP method to use ( case insensitive ).
    final Optional<String> method  = fromNullable(request.queryParamOrDefault("method", null));

    // URL path ( aka endpoint ) of the remote service.
    final Optional<String> path    = fromNullable(request.queryParamOrDefault("path", null));

    // TCP port where the remote service accepts HTTP requests.
    final Optional<Integer> port   = fromNullable(Ints.tryParse(request.queryParamOrDefault("port", ""), 10));

    // dynamic test configuration
    final Optional<String> threads = fromNullable(request.queryParamOrDefault("steps", null));
    final Optional<String> profile = fromNullable(request.queryParamOrDefault("qps", null));

    if ( (mode.equalsIgnoreCase("qps")) && !profile.isPresent() ) {
      halt(400, "'profile' is required when 'mode' is set to 'qps'");
      return null;
    }

    // expected min/max response time of the service to load test
    final int minLatency  = Integer.parseInt(request.queryParamOrDefault("min_latency", "1"), 10);
    final int maxLatency  = Integer.parseInt(request.queryParamOrDefault("max_latency", "1000"), 10);

    if ( minLatency <= 0 ) {
      halt(400, "'min_latency' must be greater than 0 milli seconds");
      return null;
    }

    if ( maxLatency < minLatency ) {
      halt(400, "'max_latency' must be greater than 'min_latency'");
      return null;
    }

    final int concurrency = Integer.parseInt(request.queryParamOrDefault("concurrency", "1"), 10);
    final int duration    = Integer.parseInt(request.queryParamOrDefault("duration", "1"), 10);
    final int rampupTime  = Integer.parseInt(request.queryParamOrDefault("rampup_time", "1"), 10);
    final int rampupSteps = Integer.parseInt(request.queryParamOrDefault("rampup_steps", "1"), 10);

    logger.info(
      toStringHelper("request")
      .add("id", testId)
      .add("output", output)
      .add("jmx", jmx)
      .add("mode", mode)
      .add("proto", proto)
      .add("method", method)
      .add("host", host)
      .add("port", port)
      .add("path", path)
      .add("threads", threads)
      .add("profile", profile)
      .add("concurrency", concurrency)
      .add("duration", duration)
      .add("rampup_time", rampupTime)
      .add("rampup_steps", rampupSteps)
      .add("min_latency", minLatency)
      .add("max_latency", maxLatency)
      .toString()
    );

    responseOutput.println("---- starting: " + testId + " ----");
    logger.info("starting: {}", testId);

    if( output != null && output.equalsIgnoreCase(SYS_OUT) ) {
      this.jMeterTestService.start(testId, jmx, mode,
        proto, method, host, port, path, threads, profile,
        concurrency, duration, rampupTime, rampupSteps,
        minLatency, maxLatency);
    } else {
      this.jMeterTestService.start(testId, jmx, mode,
        proto, method, host, port, path, threads, profile,
        concurrency, duration, rampupTime, rampupSteps,
        responseOutput, false /* closeable */,
        minLatency, maxLatency);
    }
    
    logger.info("finished: {}", testId);
    return "---- finished: " + testId + " ----";
  }

}
