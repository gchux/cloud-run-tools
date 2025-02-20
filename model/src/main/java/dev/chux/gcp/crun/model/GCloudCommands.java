package dev.chux.gcp.crun.model;

import java.util.List;

public class GCloudCommands extends Multivalue<GCloudCommand> {

  GCloudCommands() {}

  public GCloudCommands(final List<GCloudCommand> commands) {
    super(commands);
  }

}

