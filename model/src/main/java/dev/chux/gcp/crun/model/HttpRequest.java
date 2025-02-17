package dev.chux.gcp.crun.model;

import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import com.google.common.collect.ImmutableMap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;

public class HttpRequest {

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="url", alternate={"target"})
  private String url;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="method", alternate={"request", "verb", "action"})
  private String method;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="data", alternate={"payload", "body"})
  private String data;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="headers", alternate={"metadata"})
  private Map<String, String> headers;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="proxy", alternate={})
  private HttpProxy proxy;

  HttpRequest() {}
  
  public String url() {
    return emptyToNull(this.url);
  }
  
  public String method() {
    return emptyToNull(this.method);
  }
  
  public Optional<String> optionalMethod() {
    return fromNullable(emptyToNull(this.method()));
  }
  
  public String data() {
    return emptyToNull(this.data);
  }
  
  public Optional<String> optionalData() {
    return fromNullable(this.data());
  }

  public Map<String, String> headers() {
    if( this.headers == null ) {
      return ImmutableMap.of();
    } 
    return ImmutableMap.copyOf(this.headers);
  }

  public HttpProxy proxy() {
    return this.proxy;
  }

  public Optional<HttpProxy> optionalProxy() {
    return fromNullable(this.proxy());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("url", this.url())
      .add("method", this.optionalMethod())
      .add("headers", this.headers())
      .add("proxy", this.optionalProxy())
      .addValue(this.optionalData())
      .toString();
  }

}
