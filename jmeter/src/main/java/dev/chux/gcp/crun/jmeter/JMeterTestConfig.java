package dev.chux.gcp.crun.jmeter;

import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class JMeterTestConfig {

  private final String instanceID;
  private final String id;
  private Optional<String> traceID;

  private final Optional<String> jmx;
  private final String mode;
  private final Optional<String> proto;
  private final Optional<String> method;
  private final String host;
  private final Optional<Integer> port;
  private final Optional<String> path;
  private final Optional<Map<String, String>> query;
  private final Optional<Map<String, String>> headers;
  private final Optional<String> body;

  private final int minLatency;
  private final int maxLatency;

  private Optional<String> threads;
  private Optional<String> profile;

  private int concurrency = 1;
  private int duration = 1;
  private int rampupTime = 1;
  private int rampupSteps = 1;

  public JMeterTestConfig(
    @CheckForNull @NonNull final String instanceID,
    @CheckForNull @NonNull final String id,
    @CheckForNull @NonNull final String mode,
    @CheckForNull @NonNull final String host
  ) {
    this(instanceID, id, mode, host, null);
  }
  
  public JMeterTestConfig(
    @CheckForNull @NonNull final String instanceID,
    @CheckForNull @NonNull final String id,
    @CheckForNull @NonNull final String mode,
    @CheckForNull @NonNull final String host,
    @Nullable final String path
  ) {
    this(instanceID, id, null, mode, null, null, host, null, path, null, null, null, 1, 1000);
  }

  public JMeterTestConfig(
    @CheckForNull @NonNull final String instanceID,
    @CheckForNull @NonNull final String id,
    @Nullable final String jmx,
    @CheckForNull @NonNull final String mode,
    @Nullable final String proto,
    @Nullable final String method,
    @CheckForNull @NonNull final String host,
    @Nullable final Integer port,
    @Nullable final String path,
    @Nullable final Map<String, String> query,
    @Nullable final Map<String, String> headers,
    @Nullable final String body,
    final int minLatency,
    final int maxLatency
  ) {
    checkArgument(!isNullOrEmpty(instanceID), "instance_id is required");
    checkArgument(!isNullOrEmpty(id), "id is required");
    checkArgument(!isNullOrEmpty(host), "host is required");
    checkArgument(!isNullOrEmpty(mode), "mode is required");

    checkArgument(minLatency > 0, "min_latency must be greater than 0ms");
    checkArgument(maxLatency >= minLatency, "max_latency must be greater than min_latency");

    this.instanceID = instanceID;
    this.id = id;
    this.jmx = fromNullable(jmx);
    this.mode = mode;
    this.proto = fromNullable(proto);
    this.method = fromNullable(method);
    this.host = host;
    this.port = fromNullable(port);
    this.path = fromNullable(path);
    this.query = fromNullable(query);
    this.headers = fromNullable(headers);
    this.body = fromNullable(body);

    this.minLatency = minLatency;
    this.maxLatency = maxLatency;
  }

  public String instanceID() {
    return this.instanceID;
  }

  public String id() {
    return this.id;
  }

  public String mode() {
    return this.mode;
  }

  public String host() {
    return this.host;
  }

  public int minLatency() {
    return this.minLatency;
  }

  public int maxLatency() {
    return this.maxLatency;
  }

  public Optional<String> proto() {
    return this.proto;
  }

  public Optional<String> method() {
    return this.method;
  }

  public Optional<Integer> port() {
    return this.port;
  }

  public Optional<String> path() {
    return this.path;
  }

  public Optional<String> body() {
    return this.body;
  }

  public Optional<String> jmx() {
    return this.jmx;
  }

  public Optional<String> traceID() {
    return this.traceID;
  }

  public Optional<Map<String, String>> query() {
    return this.query;
  }

  public Optional<Map<String, String>> headers() {
    return this.headers;
  }

  public JMeterTestConfig traceID(
    @Nullable final String traceID
  ) {
    this.traceID = fromNullable(traceID);
    return this;
  }

  public Optional<String> threads() {
    return this.threads;
  }

  public JMeterTestConfig threads(
    @Nullable final String threads
  ) {
    this.threads = fromNullable(threads);
    return this;
  }

  public Optional<String> profile() {
    return this.profile;
  }

  public JMeterTestConfig profile(
    @Nullable final String profile
  ) {
    this.profile = fromNullable(profile);
    return this;
  }

  public int concurrency() {
    return this.concurrency;
  }

  public JMeterTestConfig concurrency(
    final int concurrency
  ) {
    this.concurrency = concurrency;
    return this;
  }

  public int duration() {
    return this.duration;
  }

  public JMeterTestConfig duration(
    final int duration
  ) {
    this.duration = duration;
    return this;
  }

  public int rampupTime() {
    return this.rampupTime;
  }

  public JMeterTestConfig rampupTime(
    final int rampupTime
  ) {
    this.rampupTime = rampupTime;
    return this;
  }

  public int rampupSteps() {
    return this.rampupSteps;
  }

  public JMeterTestConfig rampupSteps(
    final int rampupSteps
  ) {
    this.rampupSteps = rampupSteps;
    return this;
  }

}
