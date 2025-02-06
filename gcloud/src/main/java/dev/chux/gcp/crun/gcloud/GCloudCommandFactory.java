package dev.chux.gcp.crun.gcloud;

import java.io.OutputStream;

public interface GCloudCommandFactory {

  public GCloudCommand create(GCloudCommandConfig gcloudCommandConfig);

  public GCloudCommand createWithOutputStream(GCloudCommandConfig gcloudCommandConfig, OutputStream stream);

}
