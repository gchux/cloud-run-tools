package dev.chux.gcp.crun.gcloud;

import static dev.chux.gcp.crun.RestApp.of;

public class GCloudApp {

  public static void main(final String[] args) {
    final GCloudModule gcloudModule = new GCloudModule();
    of(gcloudModule).run(args);
  }

}
