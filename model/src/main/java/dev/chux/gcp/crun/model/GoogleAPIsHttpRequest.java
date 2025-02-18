package dev.chux.gcp.crun.model;

import java.util.Map;

import com.google.common.base.Optional;

import com.google.common.collect.ImmutableMap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class GoogleAPIsHttpRequest extends HttpRequest {

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="project", alternate={})
  private String projectId;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="api", alternate={"service"})
  private String api;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="version", alternate={"v"})
  private String version;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="resource", alternate={"parent", "name"})
  private String resource;

  GoogleAPIsHttpRequest() {}
  
  @Override
  public String url() {
    checkState(!isNullOrEmpty(this.api));
    checkState(!isNullOrEmpty(this.version));
    checkState(!isNullOrEmpty(this.resource));

    return new StringBuilder("https://")
      .append(this.api)
      .append(".googleapis.com/")
      .append(this.version)
      .append('/')
      .append(this.resource)
      .toString();
  }

  public String api() {
    return emptyToNull(this.api);
  }

  public String version() {
    return emptyToNull(this.version);
  }

  public String resource() {
    return emptyToNull(this.resource);
  }

  public String projectId() {
    return emptyToNull(this.projectId);
  }

  public Optional<String> optionalProjectId() {
    return fromNullable(this.projectId());
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .add("url", this.url())
      .add("method", this.optionalMethod())
      .add("headers", this.headers())
      .add("proxy", this.optionalProxy())
      .addValue(this.optionalData())
      .toString();
  }

}
