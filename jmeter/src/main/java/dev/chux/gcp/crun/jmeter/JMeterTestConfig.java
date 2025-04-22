package dev.chux.gcp.crun.jmeter;

import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class JMeterTestConfig {

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="id")
  private final String id;

  @Since(1.0)
  @Expose(deserialize=false, serialize=true)
  @SerializedName(value="name")
  private final String name;

  @Since(1.0)
  @Expose(deserialize=false, serialize=true)
  @SerializedName(value="instance_id")
  private final String instanceID;

  @Since(1.0)
  @Expose(deserialize=false, serialize=true)
  @SerializedName(value="trace_id")
  private Optional<String> traceID;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="script")
  private final Optional<String> jmx;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="mode")
  private final String mode;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="proto")
  private final Optional<String> proto;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="method")
  private final Optional<String> method;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="host")
  private final String host;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="port")
  private final Optional<Integer> port;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="path")
  private final Optional<String> path;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="params")
  private final Optional<Map<String, String>> query;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="headers")
  private final Optional<Map<String, String>> headers;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="payload")
  private final Optional<String> body;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="min_latency")
  private final int minLatency;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="max_latency")
  private final int maxLatency;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="concurrency")
  private Optional<String> threads;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="qps")
  private Optional<String> profile;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="duration")
  private int duration = 0;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="threads")
  private int concurrency = 0;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="rampup_time")
  private int rampupTime = 0;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="rampup_steps")
  private int rampupSteps = 0;

  private class Timestamps {

    @Since(1.0)
    @Expose(deserialize=false, serialize=true)
    @SerializedName(value="created")
    private final long created;

    @Since(1.0)
    @Expose(deserialize=false, serialize=true)
    @SerializedName(value="started")
    private long started = 0;

    @Since(1.0)
    @Expose(deserialize=false, serialize=true)
    @SerializedName(value="finished")
    private long finished = 0;

    private Timestamps(
      final long created
    ) {
      this.created = created;
    }

    private Timestamps() {
      this(System.currentTimeMillis());
    }

  }

  @Since(1.0)
  @Expose(deserialize=false, serialize=true)
  @SerializedName(value="timestamps")
  private final Timestamps timestamps;

  public JMeterTestConfig(
    @CheckForNull @NonNull final String name,
    @CheckForNull @NonNull final String instanceID,
    @CheckForNull @NonNull final String id,
    @CheckForNull @NonNull final String mode,
    @CheckForNull @NonNull final String host
  ) {
    this(name, instanceID, id, mode, host, null);
  }
  
  public JMeterTestConfig(
    @CheckForNull @NonNull final String name,
    @CheckForNull @NonNull final String instanceID,
    @CheckForNull @NonNull final String id,
    @CheckForNull @NonNull final String mode,
    @CheckForNull @NonNull final String host,
    @Nullable final String path
  ) {
    this(name, instanceID, id, null, mode, null, null, host, null, path, null, null, null, 1, 1000);
  }

  public JMeterTestConfig(
    @CheckForNull @NonNull final String name,
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
    checkArgument(!isNullOrEmpty(name), "name is required");
    checkArgument(!isNullOrEmpty(instanceID), "instance_id is required");
    checkArgument(!isNullOrEmpty(id), "id is required");
    checkArgument(!isNullOrEmpty(host), "host is required");
    checkArgument(!isNullOrEmpty(mode), "mode is required");

    checkArgument(minLatency > 0, "min_latency must be greater than 0ms");
    checkArgument(maxLatency >= minLatency, "max_latency must be greater than min_latency");

    this.name = name;
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

    this.timestamps = new Timestamps();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .add("instance", this.instanceID())
      .add("name", this.name())
      .add("id", this.id())
      .add("jmx", this.jmx())
      .add("mode", this.mode())
      .add("host", this.host())
      .toString();
  }

  public String name() {
    return this.name;
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

  public long started() {
    return this.timestamps.started;
  }

  public JMeterTestConfig started(
    final long timestamp
  ) {
    this.timestamps.started = timestamp;
    return this;
  }

  public long finished() {
    return this.timestamps.finished;
  }

  public JMeterTestConfig finished(
    final long timestamp
  ) {
    this.timestamps.finished = timestamp;
    return this;
  }

}
