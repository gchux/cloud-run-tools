package dev.chux.gcp.crun.gcloud;

import java.io.OutputStream;

public interface GCloudCommandFactory {

  public GCloudCommand create(final dev.chux.gcp.crun.model.GCloudCommand gcloudCommand);

  public GCloudCommand createWithOutputStream(final dev.chux.gcp.crun.model.GCloudCommand gcloudCommand, OutputStream stream);

}
