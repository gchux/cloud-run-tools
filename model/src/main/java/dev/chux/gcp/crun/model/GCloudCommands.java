package dev.chux.gcp.crun.model;

import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class GCloudCommands implements Supplier<List<GCloudCommand>> {

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="commands", alternate={"cmds"})
  private List<GCloudCommand> commands;

  GCloudCommands() {}

  public GCloudCommands(final List<GCloudCommand> commands) {
    this.commands = checkNotNull(commands);
  }
  
  public List<GCloudCommand> commands() {
    if( this.commands == null ) {
      return ImmutableList.of();
    } 
    return ImmutableList.copyOf(this.commands);
  }

  @Override
  public List<GCloudCommand> get() {
    return this.commands();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .add("commands", this.commands())
      .toString();
  }

}

