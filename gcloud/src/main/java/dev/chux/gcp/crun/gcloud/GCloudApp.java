package dev.chux.gcp.crun.gcloud;

import dev.chux.gcp.crun.App;

public class GCloudApp {

  public static void main(final String[] args) {
    final GCloudModule gcloudModule = new GCloudModule();
    final App app = new App(gcloudModule);
    app.run(args);
  }

}
