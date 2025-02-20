package dev.chux.gcp.crun.model;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;

public class GCloudCommand {

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="namespace", alternate={"ns"})
  private String namespace;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="groups", alternate={"g"})
  private List<String> groups;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="command", alternate={"cmd"})
  private String command;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="flags", alternate={"fl"})
  private Map<String, String> flags;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="arguments", alternate={"args"})
  private List<String> arguments;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="format", alternate={"fmt"})
  private String format;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="project", alternate={"parent"})
  private String project;

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="environment", alternate={"env"})
  private Map<String, String> environment;

  public GCloudCommand() {}
  
  public String namespace() {
    return this.namespace;
  }

  public Optional<String> optionalNamespace() {
    return fromNullable(emptyToNull(this.namespace));
  }

  public void namespace(final String namespace) {
    this.namespace = namespace;
  }

  public String command() {
    return this.command;
  }

  public Optional<String> optionalCommand() {
    return fromNullable(emptyToNull(this.command));
  }

  public String format() {
    return emptyToNull(this.format);
  }

  public Optional<String> optionalFormat() {
    return fromNullable(this.format());
  }

  public String project() {
    return emptyToNull(this.project);
  }

  public Optional<String> optionalProject() {
    return fromNullable(this.project());
  }

  public List<String> groups() {
    if( this.groups == null ) {
      return ImmutableList.of();
    } 
    return ImmutableList.copyOf(this.groups);
  }

  public List<String> arguments() {
    if( this.arguments == null ) {
      return ImmutableList.of();
    } 
    return ImmutableList.copyOf(this.arguments);
  }

  public Map<String, String> flags() {
    if( this.flags == null ) {
      return ImmutableMap.of();
    } 
    return ImmutableMap.copyOf(this.flags);
  }

  public Map<String, String> environment() {
    if( this.environment == null ) {
      return ImmutableMap.of();
    } 
    return ImmutableMap.copyOf(this.environment);
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .add("project", this.optionalProject())
      .add("namespace", this.optionalNamespace())
      .add("groups", this.groups())
      .add("flags", this.flags())
      .add("format", this.optionalFormat())
      .add("command", this.optionalCommand())
      .add("arguments", this.arguments())
      .add("environment", this.environment())
      .toString();
  }

}
