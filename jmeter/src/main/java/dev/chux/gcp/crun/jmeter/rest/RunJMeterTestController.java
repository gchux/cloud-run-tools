package dev.chux.gcp.crun.jmeter.rest;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
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
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

import static spark.Spark.*;

public class RunJMeterTestController extends JMeterTestController {

  private static final Logger logger = LoggerFactory.getLogger(RunJMeterTestController.class);

  private final JMeterTestService jMeterTestService;
  private final Set<String> modes;
  private final String instanceID;

  private final AtomicBoolean lock = new AtomicBoolean(false);

  private static class Callback implements FutureCallback<JMeterTest>, Supplier<String> {

    private static final Logger logger = LoggerFactory.getLogger(Callback.class);

    private final AtomicBoolean lock;
    private final String instanceID;
    private final String testID;

    private Callback(
      final AtomicBoolean lock,
      final String instanceID,
      final String testID
    ) {
      this.lock = lock;
      this.instanceID = instanceID;
      this.testID = testID;
    }

    private void release() {
      if ( this.lock.compareAndSet(true, false) ) {
        logger.info("{} | is now available", this.instanceID);
      } else {
        logger.error("{} | is not available", this.instanceID);
      }
    }

    private void always(
      final boolean success,
      final Optional<JMeterTest> test,
      final Optional<Throwable> error
    ) {
      if ( success ) {
        final JMeterTest t = test.get();
        logger.info("finished/SUCCESS: {}/{}", t.instanceID(), t.id());
      } else {
        logger.error("finished/FAILED: {}/{}", this.instanceID, this.get());
      }
      this.release();
    }

    @Override
    public void onSuccess(final JMeterTest t) {
      this.always(/* success */ true, fromNullable(t), absent());
    }

    @Override
    public void onFailure(final Throwable t) {
      this.always(/* success */ false, absent(), fromNullable(t));
    }

    @Override
    public String get() {
      return this.testID;
    }

    @Override
    public String toString() {
      return toStringHelper(this)
        .add("instance_id", this.instanceID)
        .add("test_id", this.get())
        .toString();
    }

  }

  private static final class AsyncCallback implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCallback.class);

    private final String instanceID;
    private final String testID;
    private final ListenableFuture<JMeterTest> test;

    private AsyncCallback(
      final String instanceID,
      final String testID,
      ListenableFuture<JMeterTest> test
    ) {
      this.instanceID = instanceID;
      this.testID = testID;
      this.test = test;
    }

    @Override
    public void run() {
      try {
        final JMeterTest t = this.test.get();
        logger.info("async/finished/OK: {}/{}", t.instanceID(), t.id());
      } catch(final Exception e) {
        logger.error("async/finished/KO: {}/{}\n{}",
          this.instanceID, this.testID, getStackTraceAsString(e));
      }
    }

  }

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

  @Override
  public void register(final String basePath) {
    register(basePath, "run");
    path(apiBase(), () -> {
      get("/run", "*/*", this);
      get("/run/:id", "*/*", this);

      post("/run", "*/*", this);
      post("/run/:id", "*/*", this);

      put("/run", "*/*", this);
      put("/run/:id", "*/*", this);

      patch("/run", "*/*", this);
      patch("/run/:id", "*/*", this);

      head("/run", "*/*", this);
      head("/run/:id", "*/*", this);

      delete("/run", "*/*", this);
      delete("/run/:id", "*/*", this);

      options("/run", "*/*", this);
      options("/run/:id", "*/*", this);
    });
  }

  @Override
  public String endpoint(final String basePath) {
    return "[GET|POST] " + apiPath();
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

  protected final Object badRequest(
    final Response response,
    final String message
  ) {
    // ALWAYS unlock before halting a request!
    this.lock.set(false);
    return super.badRequest(response, message);
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    // allows at most 1 execution per worker
    if ( !this.lock.compareAndSet(false, true) ) {
      logger.error("Worker is busy: {} => {}", this.instanceID, this.lock);
      response.status(409);
      return "busy";
    }
    
    final Optional<String> body = body(request);
    final ServletOutputStream responseOutput = response.raw().getOutputStream();

    final String output = output(request);
    final String testID = id(request);

    final Optional<String> traceID = traceID(request);

    // FQDN, hostname or IP of the remote service.
    final String host = host(request);
    if ( isNullOrEmpty(host) ) {
      return this.badRequest(response, "host is required");
    }

    // Operation mode of the Load Test, may be: `qps` or `concurrency`.
    final String mode = mode(request);
    if ( isNullOrEmpty(mode) ) {
      return this.badRequest(response, "mode is required");
    }
    if ( !this.modes.contains(mode) ) {
      return this.badRequest(response, "invalid mode: " + mode);
    }

    final boolean async = async(request);

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
      return this.badRequest(response, "parameter 'qps' is required when 'mode' is set to 'qps'");
    }

    if ( (mode.equalsIgnoreCase(MODE_CONCURRENCY)) && !concurrency.isPresent() ) {
      return this.badRequest(response, "parameter 'concurrency' is required when 'mode' is set to 'concurrency'");
    }

    // expected min/max response time of the service to load test
    final int minLatency = minLatency(request);
    final int maxLatency = maxLatency(request);

    if ( minLatency <= 0 ) {
      return this.badRequest(response, "'min_latency' must be greater than 0 milli seconds");
    }

    if ( maxLatency < minLatency ) {
      return this.badRequest(response, "'max_latency' must be greater than 'min_latency'");
    }

    final int duration = duration(request);
    if ( duration <= 0 ) {
      return this.badRequest(response, "duration must be greater than 0");
    }

    // legacy test parameters
    final int threads     = threads(request);
    final int rampupTime  = rampupTime(request);
    final int rampupSteps = rampupSteps(request);

    logger.info(
      toStringHelper(testID)
      .add("async", async)
      .add("instance", this.instanceID)
      .add("trace_id", traceID)
      .add("output", output)
      .add("script", jmx)
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

    final Callback cb = new Callback(this.lock, this.instanceID, testID);

    logger.info("starting: {}/{}", this.instanceID, testID);

    final ListenableFuture<JMeterTest> test;
    if( async || output.equalsIgnoreCase(SYS_OUT) ) {
      test = this.jMeterTestService.start(cb,
        this.instanceID, testID, traceID,
        jmx, mode, proto, method, host, port, path,
        query, headers, body, concurrency, qps,
        threads, duration, rampupTime, rampupSteps,
        minLatency, maxLatency);
    } else {
      test = this.jMeterTestService.start(cb,
        this.instanceID, testID, traceID,
        jmx, mode, proto, method, host, port, path,
        query, headers, body, concurrency, qps,
        threads, duration, rampupTime, rampupSteps,
        responseOutput, false /* closeable */,
        minLatency, maxLatency);
    }

    setHeader(response, "id", testID);
    setHeader(response, "instance-id", this.instanceID);
    if ( traceID.isPresent() ) {
      setHeader(response, "trace-id", traceID.get());
    }
    setHeader(response, "status", appendToBase("status/" + testID));
    setHeader(response, "stream", appendToBase("stream/" + testID));

    if ( async || test.isDone() || test.isCancelled() ) {
      this.jMeterTestService.executor(testID)
        .execute(new AsyncCallback(this.instanceID, testID, test));
      response.status(204);
      return "";
    }

    response.type("text/plain");

    responseOutput.println("---- test/start: <" + testID + "> ----");
    responseOutput.flush();
    final JMeterTest t = test.get();
    responseOutput.println("---- test/stopped: <" + t.id() + "> ----");
    responseOutput.flush();
    return null;
  }

}
