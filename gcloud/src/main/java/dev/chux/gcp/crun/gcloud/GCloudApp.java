package dev.chux.gcp.crun.gcloud;

import static dev.chux.gcp.crun.App.newApp;

public class GCloudApp {

  public static void main(final String[] args) {
    final GCloudModule gcloudModule = new GCloudModule();
    newApp(gcloudModule).run(args);
  }

}
