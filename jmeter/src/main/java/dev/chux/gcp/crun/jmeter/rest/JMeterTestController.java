package dev.chux.gcp.crun.jmeter.rest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.rest.RestController;

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

abstract class JMeterTestController extends RestController {

  public static final String PROPERTY_JMETER_MODES = "jmeter.modes";

  public static final String DEFAULT_TRACE_ID = "00000000000000000000000000000000";

  protected static final String SYS_OUT = "sys";
  protected static final String RES_OUT = "res";

  protected static final String MODE_QPS = "qps";
  protected static final String MODE_CONCURRENCY = "concurrency";

  private static final Splitter TRACE_SPLITTER = 
    Splitter.on(CharMatcher.anyOf("/;-"))
      .trimResults().omitEmptyStrings().limit(3);

  private static final Splitter METADATA_SPLITTER =
    Splitter.on(CharMatcher.anyOf(":="))
      .trimResults().omitEmptyStrings().limit(2);
  
  private static final Splitter.MapSplitter METADATA_MAP_SPLITTER =
    Splitter.on(CharMatcher.is(';'))
      .trimResults().omitEmptyStrings()
      .withKeyValueSeparator(METADATA_SPLITTER);

  private static final Joiner HEADER_JOINER = Joiner.on('-').skipNulls();

  private static final CharMatcher UNDERSCORE = CharMatcher.is('_');

  private static final String HEADER_PREFIX = "x-jmaas-test";

  private static final Integer INTEGER_0 = Integer.valueOf(0);
  private static final Integer INTEGER_1 = Integer.valueOf(1);

  private static final Integer DEFAULT_MIN_LATENCY = Integer.valueOf(1);
  private static final Integer DEFAULT_MAX_LATENCY = Integer.valueOf(1000);

  private static final String DEFAULT_TRACE_CONTEXT = DEFAULT_TRACE_ID + "/0000000000000000;o=0";

  protected JMeterTestController() {}

  protected final void register(
    final String root,
    final String path
  ) {
    register(root, RestModule.API_BASE, path);
  }

  protected final Set<String> jmeterModes(
    final ConfigService configService
  ) {
    return ImmutableSet.copyOf(jmeterModesProperty(configService));
  }
  
  protected final List<String> jmeterModesProperty(
    final ConfigService configService
  ) {
    return configService.getMultivalueAppProp(PROPERTY_JMETER_MODES);
  }

  protected final String toHeaderName(final String param) {
    return HEADER_JOINER.join(
      HEADER_PREFIX,
      UNDERSCORE.replaceFrom(param, '-')
    );
  }

  protected final Optional<Integer> optionalIntParam(
    final Request request,
    final String param
  ) {
    final Optional<String> value = this.optionalParam(request, param);
    if ( value.isPresent() ) {
      return fromNullable(
        Ints.tryParse(value.get())
      );
    }
    return Optional.<Integer>absent();
  }

  protected final int optionalIntParamOr(
    final Request request,
    final String param,
    final Integer value
  ) {
    return optionalIntParam(request, param).or(value).intValue();
  }

  protected final Optional<Boolean> optionalBoolParam(
    final Request request,
    final String param
  ) {
    final Optional<String> value = this.optionalParam(request, param);
    if ( value.isPresent() ) {
      return fromNullable(
        Boolean.valueOf(value.get())
      );
    }
    return Optional.<Boolean>absent();
  }

  protected final boolean optionalBoolParamOr(
    final Request request,
    final String param,
    final boolean value
  ) {
    return optionalBoolParam(request, param)
      .or(Boolean.valueOf(value)).booleanValue();
  }

  protected final Optional<String> optionalParam(
    final Request request,
    final String param
  ) {
    return fromNullable(
      emptyToNull(
        request.queryParamOrDefault(param, null)
      )
    ).or(
      fromNullable(
        emptyToNull(
          request.headers(
            toHeaderName(param)
          )
        )
      )
    ).or(
      fromNullable(
        emptyToNull(
          request.params(":" + param)
        )
      )
    );
  }

  protected final String optionalParamOr(
    final Request request,
    final String param,
    final String value
  ) {
    return this.optionalParam(request, param).or(value);
  }

  protected final Map<String, String> metadata(
    final Request request,
    final String param
  ) {
    final Optional<String> metadata = this.optionalParam(request, param);
    if ( metadata.isPresent() ) {
      return METADATA_MAP_SPLITTER.split(metadata.get());
    }
    return ImmutableMap.of();
  }
  
  protected final String host(final Request request) {
    return this.optionalParam(request, "host").orNull();
  }

  protected final boolean async(final Request request) {
    return this.optionalBoolParamOr(request, "async", false);
  }

  protected final Optional<String> optionalID(final Request request) {
    return this.optionalParam(request, "id");
  }

  protected final String id(final Request request) {
    return this.optionalID(request).or(UUID.randomUUID().toString());
  }

  protected final String mode(final Request request) {
    return this.optionalParamOr(request, "mode", MODE_CONCURRENCY).toLowerCase();
  }

  protected final String output(final Request request) {
    return this.optionalParamOr(request, "output", RES_OUT).toLowerCase();
  }

  protected final Optional<String> name(final Request request) {
    return this.optionalParam(request, "name");
  }

  protected final Optional<String> script(final Request request) {
    return this.optionalParam(request, "script");
  }

  protected final Optional<String> proto(final Request request) {
    return this.optionalParam(request, "proto");
  }

  protected final Optional<Integer> port(final Request request) {
    return this.optionalIntParam(request, "port");
  }

  protected final Optional<String> endpoint(final Request request) {
    return this.optionalParam(request, "path");
  }

  protected final Optional<String> method(final Request request) {
    return this.optionalParam(request, "method")
            .or(fromNullable(requestMethod(request)));
  }

  protected final Optional<String> concurrency(final Request request) {
    return this.optionalParam(request, MODE_CONCURRENCY);
  }

  protected final Optional<String> qps(final Request request) {
    return this.optionalParam(request, MODE_QPS);
  }

  protected final int minLatency(final Request request) {
    return this.optionalIntParamOr(request, "min_latency", DEFAULT_MIN_LATENCY);
  }

  protected final int maxLatency(final Request request) {
    return this.optionalIntParamOr(request, "max_latency", DEFAULT_MAX_LATENCY);
  }

  protected final int duration(final Request request) {
    return this.optionalIntParamOr(request, "duration", INTEGER_0);
  }

  protected final int threads(final Request request) {
    return this.optionalIntParamOr(request, "threads", INTEGER_0);
  }

  protected final int rampupTime(final Request request) {
    return this.optionalIntParamOr(request, "rampup_time", INTEGER_0);
  }

  protected final int rampupSteps(final Request request) {
    return this.optionalIntParamOr(request, "rampup_steps", INTEGER_0);
  }

  protected final Optional<String> traceID(final Request request) {
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
      return Optional.of(DEFAULT_TRACE_ID);
    }

    if  ( traceparent.isPresent() ) {
      // trace context extracted from `traceparent`
      // sample: `00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01`
      return fromNullable(emptyToNull(parts.get(1)));
    }
    // trace context extracted from `x-cloud-trace-context`
    return fromNullable(emptyToNull(parts.get(0)));
  }

  protected final Map<String, String> params(final Request request) {
    return metadata(request, "params");
  }

  protected final Map<String, String> headers(final Request request) {
    return metadata(request, "headers");
  }

  protected final Optional<String> body(final Request request) {
    return fromNullable(emptyToNull(request.body()));
  }

  protected final void setHeader(
    final Response response,
    final String name,
    final String value
  ) {
    if ( !isNullOrEmpty(value) ) {
      response.header(
        toHeaderName(name), value
      );
    }
  }

  protected Object badRequest(
    final Response response,
    final String message
  ) {
    halt(400, message);
    return null;
  }

}

