package dev.chux.gcp.crun.jmeter.rest;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.jmeter.JMeterTest;
import dev.chux.gcp.crun.jmeter.JMeterTestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.isNullOrEmpty;

import static spark.Spark.*;

public class RunJMeterTestController extends JMeterTestController {

  private static final Logger logger = LoggerFactory.getLogger(RunJMeterTestController.class);

  private final JMeterTestService jMeterTestService;
  private final Set<String> modes;
  private final String instanceID;

  private final AtomicBoolean busy = new AtomicBoolean(false);

  @Inject
  public RunJMeterTestController(
    final ConfigService configService,
    final JMeterTestService jMeterTestService
  ) {
    this.jMeterTestService = jMeterTestService;
    this.modes = this.jmeterModes(configService);
    this.instanceID = this.instanceID(configService);
    logger.info("com.google.cloud.run.instance.id={}", this.instanceID);
  }

  public void register(final String basePath) {
    path(basePath, () -> {
      path("/jmeter", () -> {
        path("/test", () -> {
          get("/run", "*/*", this);
          get("/run/:id", "*/*", this);
          post("/run", "*/*", this);
          post("/run/:id", "*/*", this);
        });
      });
    });
  }

  public String endpoint(final String basePath) {
    return "[GET|POST] " + basePath + "/jmeter/test/run";
  }

  private String instanceID(
    final ConfigService configService
  ) {
    return configService
      .getOptionalSysProp("com.google.cloud.run.instance.id")
      .or(this.newInstanceID());
  }

  private String newInstanceID() {
    final UUID uuid = UUID.randomUUID();
    return Hashing.sha256().newHasher()
      .putLong(uuid.getMostSignificantBits())
      .putLong(uuid.getLeastSignificantBits())
      .putLong(System.nanoTime())
      .hash().toString();
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    // allows at most 1 execution per worker
    if ( !this.busy.compareAndSet(false, true) ) {
      response.status(409);
      return "busy";
    }
    
    final Optional<String> body = body(request);
    final ServletOutputStream responseOutput = response.raw().getOutputStream();

    final String output = output(request);
    final String testID = id(request);

    response.type("text/plain");
    setHeader(response, "id", testID);
    setHeader(response, "instance-id", this.instanceID);

    final Optional<String> traceID = traceID(request);
    if ( traceID.isPresent() ) {
      setHeader(response, "trace-id", traceID.get());
    }

    // FQDN, hostname or IP of the remote service.
    final String host = host(request);
    if ( isNullOrEmpty(host) ) {
      response.status(400);
      return "host is required";
    }

    // Operation mode of the Load Test, may be: `qps` or `concurrency`.
    final String mode = mode(request);
    if ( isNullOrEmpty(mode) ) {
      halt(400, "mode is required");
      return null;
    }
    if ( !this.modes.contains(mode) ) {
      halt(400, "invalid mode: " + mode);
      return null;
    }

    // test to execute base on the name of JMX files ( case sensitive ).
    final Optional<String> jmx         = script(request);

    // may be `http` ot `https` ( case insensitive ).
    final Optional<String> proto       = proto(request);

    // HTTP method to use ( case insensitive ).
    final Optional<String> method      = method(request);

    // URL path ( aka endpoint ) of the remote service.
    final Optional<String> path        = endpoint(request);

    // request metadata ( query params, and headers )
    final Map<String, String> query    = params(request);
    final Map<String, String> headers  = headers(request);

    // TCP port where the remote service accepts HTTP requests.
    final Optional<Integer> port       = port(request);

    // dynamic test configuration
    final Optional<String> concurrency = concurrency(request);
    final Optional<String> qps         = qps(request);

    if ( (mode.equalsIgnoreCase(MODE_QPS)) && !qps.isPresent() ) {
      halt(400, "parameter 'qps' is required when 'mode' is set to 'qps'");
      return null;
    }

    if ( (mode.equalsIgnoreCase(MODE_CONCURRENCY)) && !concurrency.isPresent() ) {
      halt(400, "parameter 'concurrency' is required when 'mode' is set to 'concurrency'");
      return null;
    }

    // expected min/max response time of the service to load test
    final int minLatency = minLatency(request);
    final int maxLatency = maxLatency(request);

    if ( minLatency <= 0 ) {
      halt(400, "'min_latency' must be greater than 0 milli seconds");
      return null;
    }

    if ( maxLatency < minLatency ) {
      halt(400, "'max_latency' must be greater than 'min_latency'");
      return null;
    }

    final int duration = duration(request);
    if ( duration <= 0 ) {
      halt(400, "duration must be greater than 0");
      return null;
    }

    // legacy test parameters
    final int threads     = threads(request);
    final int rampupTime  = rampupTime(request);
    final int rampupSteps = rampupSteps(request);

    logger.info(
      toStringHelper(testID)
      .add("instance", this.instanceID)
      .add("trace_id", traceID)
      .add("output", output)
      .add("test", jmx)
      .add("mode", mode)
      .add("proto", proto)
      .add("method", method)
      .add("host", host)
      .add("port", port)
      .add("path", path)
      .add("query", query)
      .add("headers", headers)
      .add("body", body)
      .add(MODE_QPS, qps)
      .add(MODE_CONCURRENCY, concurrency)
      .add("min_latency", minLatency)
      .add("max_latency", maxLatency)
      .add("duration", duration)
      .add("threads", threads)
      .add("rampup_time", rampupTime)
      .add("rampup_steps", rampupSteps)
      .toString()
    );

    responseOutput.println("---- test/start: <" + testID + "> ----");
    logger.info("starting: {}/{}", this.instanceID, testID);

    
    final ListenableFuture<JMeterTest> jMeterTest;
    if( output != null && output.equalsIgnoreCase(SYS_OUT) ) {
      jMeterTest = this.jMeterTestService.start(
        this.instanceID, testID, traceID,
        jmx, mode, proto, method, host, port, path,
        query, headers, body, concurrency, qps,
        threads, duration, rampupTime, rampupSteps,
        minLatency, maxLatency);
    } else {
      jMeterTest = this.jMeterTestService.start(
        this.instanceID, testID, traceID,
        jmx, mode, proto, method, host, port, path,
        query, headers, body, concurrency, qps,
        threads, duration, rampupTime, rampupSteps,
        responseOutput, false /* closeable */,
        minLatency, maxLatency);
    }
    responseOutput.flush();
    final JMeterTest test = jMeterTest.get();

    logger.info("finished: {}/{}", this.instanceID, test.id());
    this.busy.set(false);
    responseOutput.println("---- test/stop: <" + testID + "> ----");
    responseOutput.flush();
    return null;
  }

}
