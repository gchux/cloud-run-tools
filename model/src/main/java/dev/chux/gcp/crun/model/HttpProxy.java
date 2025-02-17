package dev.chux.gcp.crun.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;

public class HttpProxy {

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="host", alternate={})
  private String host;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="port", alternate={})
  private Integer port;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="secure", alternate={"https"})
  private Boolean secure;

  HttpProxy() {}
  
  public String host() {
    return emptyToNull(this.host);
  }
  
  public Integer port() {
    return this.port;
  }
  
  public Boolean isSecure() {
    return (this.secure == null) ? Boolean.FALSE : this.secure;
  }
  
  public Optional<Boolean> optionalIsSecure() {
    return fromNullable(this.secure);
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("host", this.host())
      .add("port", this.port())
      .add("secure", this.optionalIsSecure())
      .toString();
  }

}
