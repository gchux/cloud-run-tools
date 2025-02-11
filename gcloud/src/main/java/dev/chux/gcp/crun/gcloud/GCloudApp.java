package dev.chux.gcp.crun.gcloud;

import static dev.chux.gcp.crun.App.newRestApp;

public class GCloudApp {

  public static void main(final String[] args) {
    final GCloudModule gcloudModule = new GCloudModule();
    newRestApp(gcloudModule).runRestAPI(args);
  }

}
