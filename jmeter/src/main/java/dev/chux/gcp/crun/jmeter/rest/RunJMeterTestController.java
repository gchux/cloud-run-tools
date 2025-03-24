package dev.chux.gcp.crun.jmeter.rest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.rest.Route;
import dev.chux.gcp.crun.jmeter.JMeterTestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

import static spark.Spark.*;

public class RunJMeterTestController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunJMeterTestController.class);

  public static final String PROPERTY_JMETER_MODES = "jmeter.modes";

  static final CharMatcher TRACE_MATCHER = CharMatcher.anyOf("/;-");
  static final Splitter TRACE_SPLITTER = Splitter.on(TRACE_MATCHER).trimResults().omitEmptyStrings().limit(3);

  private static final String SYS_OUT = "sys";
  private static final String RES_OUT = "res";

  private static final String DEFAULT_TRACE_ID = "00000000000000000000000000000000";
  private static final String DEFAULT_TRACE_CONTEXT = DEFAULT_TRACE_ID + "/0000000000000000;o=0";

  private final JMeterTestService jMeterTestService;
  private final Set<String> modes;
  private final String instanceID;

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

  private Set<String> jmeterModes(
    final ConfigService configService
  ) {
    return ImmutableSet.copyOf(jmeterModesProperty(configService));
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
    final Hasher h = Hashing.sha256().newHasher();
    return h
      .putLong(uuid.getMostSignificantBits())
      .putLong(uuid.getLeastSignificantBits())
      .putLong(System.nanoTime())
      .hash().toString();
  }

  private String newID(final String testID) {
    final Hasher h = Hashing.crc32c().newHasher();
    return h.putString(testID, UTF_8)
      .putLong(System.currentTimeMillis())
      .hash().toString();
  }
  
  private final List<String> jmeterModesProperty(
    final ConfigService configService
  ) {
    return configService.getMultivalueAppProp(PROPERTY_JMETER_MODES);
  }

  private String traceID(final Request request) {
    final Optional<String> xCloudTraceCtx = fromNullable(
      emptyToNull(request.headers("x-cloud-trace-context"))
    );
    final Optional<String> traceparent = fromNullable(
      emptyToNull(request.headers("traceparent"))
    );
    final String traceContext = firstNonNull(
      traceparent.orNull(),
      xCloudTraceCtx.or(DEFAULT_TRACE_CONTEXT)
    );

    final List<String> parts = TRACE_SPLITTER.splitToList(traceContext);
    if ( parts.size() < 2 ) {
      return DEFAULT_TRACE_ID;
    }

    if  ( traceparent.isPresent() ) {
      // trace context extracted from `traceparent`
      // sample: `00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01`
      return emptyToNull(parts.get(1));
    }
    // trace context extracted from `x-cloud-trace-context`
    return emptyToNull(parts.get(0));
  }

  private final Path writeRequestFile(
    final String testID,
    final Optional<String> method,
    final String host,
    final Optional<String> path,
    final String body
  ) {
    final String lineBreak = System.lineSeparator();

    // ToDo:
    //   1. add support for more headers and query params
    //   2. move request file builder to its own `class`
    final StringBuilder content = new StringBuilder(lineBreak)
      .append(method.or("get").toUpperCase())
        .append(' ').append(path.or("/")).append("HTTP/1.1")
    .append(lineBreak)
      .append("Host: ").append(host)
    .append(lineBreak)
      .append("Connection: keep-alive")
    .append(lineBreak)
      .append("x-jmaas-test-id: ").append(testID)
    .append(lineBreak)
    .append(lineBreak)
      .append(body)
    .append(lineBreak);
    
    content.insert(0, content.length()-lineBreak.length());

    logger.info("request file: {}", content);

    try {
      final Path filePath = Paths.get("/tmp/" + this.newID(testID));
      return Files.writeString(filePath, content, UTF_8);
    } catch(final Exception e) {
      logger.error("failed to write request file: {}", getStackTraceAsString(e));
    }
    return null;
  }

  private final boolean deleteRequestFile(
    final Optional<Path> requestFilePath
  ) {
    if ( requestFilePath.isPresent() ) {
      try {
        return Files.deleteIfExists(requestFilePath.get());
      } catch(final Exception e) {
        logger.error("failed to delete request file: {}", getStackTraceAsString(e));
      }
    }
    return false;
  }

  public void register(final String basePath) {
    path(basePath, () -> {
      path("/jmeter", () -> {
        path("/test", () -> {
          get("/run", this);
          post("/run", "*/*", this);
        });
      });
    });
  }

  public String endpoint(final String basePath) {
    return "GET " + basePath + "/jmeter/test/run";
  }

  public Object handle(final Request request, final Response response) throws Exception {
    final String body = request.body();

    final Optional<String> traceID = fromNullable(this.traceID(request));
    final String output = request.queryParamOrDefault("output", RES_OUT); 
    final ServletOutputStream responseOutput = response.raw().getOutputStream();

    final Optional<String> id = fromNullable(request.queryParamOrDefault("id", null));
    final String testID = id.or(UUID.randomUUID().toString());

    response.type("text/plain");
    response.header("x-jmaas-test-id", testID);
    if ( traceID.isPresent() ) {
      response.header("x-cloud-trace-id", traceID.get());
    }

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
    final Optional<String> jmx     = fromNullable(request.queryParamOrDefault("test", null));
    //
    // may be `http` ot `https` ( case insensitive ).
    final Optional<String> proto   = fromNullable(request.queryParamOrDefault("proto", null));

    // HTTP method to use ( case insensitive ).
    final Optional<String> method  = fromNullable(request.queryParamOrDefault("method", null));

    // URL path ( aka endpoint ) of the remote service.
    final Optional<String> path    = fromNullable(request.queryParamOrDefault("path", null));

    // TCP port where the remote service accepts HTTP requests.
    final Optional<Integer> port   = fromNullable(Ints.tryParse(request.queryParamOrDefault("port", ""), 10));

    final Optional<Path> requestFilePath = fromNullable(
      this.writeRequestFile(testID, method, host, path, body)
    );

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

    final int duration    = Integer.parseInt(request.queryParamOrDefault("duration", "1"), 10);
    final int concurrency = Integer.parseInt(request.queryParamOrDefault("concurrency", "1"), 10);
    final int rampupTime  = Integer.parseInt(request.queryParamOrDefault("rampup_time", "1"), 10);
    final int rampupSteps = Integer.parseInt(request.queryParamOrDefault("rampup_steps", "1"), 10);

    logger.info(
      toStringHelper(testID)
      .add("instance", this.instanceID)
      .add("output", output)
      .add("test", jmx)
      .add("mode", mode)
      .add("proto", proto)
      .add("method", method)
      .add("host", host)
      .add("port", port)
      .add("path", path)
      .add("steps", threads)
      .add("qps", profile)
      .add("concurrency", concurrency)
      .add("duration", duration)
      .add("rampup_time", rampupTime)
      .add("rampup_steps", rampupSteps)
      .add("min_latency", minLatency)
      .add("max_latency", maxLatency)
      .toString()
    );

    responseOutput.println("---- starting: " + testID + " ----");
    logger.info("starting: {}", testID);

    if( output != null && output.equalsIgnoreCase(SYS_OUT) ) {
      this.jMeterTestService.start(this.instanceID, testID, traceID,
        jmx, mode, proto, method, host, port, path, threads, profile,
        concurrency, duration, rampupTime, rampupSteps,
        minLatency, maxLatency);
    } else {
      this.jMeterTestService.start(this.instanceID, testID, traceID,
        jmx, mode, proto, method, host, port, path, threads, profile,
        concurrency, duration, rampupTime, rampupSteps,
        responseOutput, false /* closeable */,
        minLatency, maxLatency);
    }

    if ( this.deleteRequestFile(requestFilePath) ) {
      logger.info("deleted request file: {}", requestFilePath.get());
    }
    
    logger.info("finished: {}", testID);
    return "---- finished: " + testID + " ----";
  }

}
