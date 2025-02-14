package dev.chux.gcp.crun.gcloud;

import java.io.OutputStream;
import java.util.function.Consumer;

import com.google.inject.Inject;

import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

import static com.google.common.base.Preconditions.checkNotNull;

public class GCloudService {

  private final GCloudCommandFactory gcloudCommandFactory;
  private final Consumer<ManagedProcessProvider> processConsumer;
  
  @Inject
  GCloudService(
    GCloudCommandFactory gcloudCommandFactory,
    @ProcessConsumer Consumer<ManagedProcessProvider> processConsumer
  ) {
    this.gcloudCommandFactory = gcloudCommandFactory;
    this.processConsumer = processConsumer;
  }

  public void run(final GCloudCommandConfig config) {
    final GCloudCommand gcloudCommand =
      this.gcloudCommandFactory.create(checkNotNull(config));
    this.run(gcloudCommand);
  }

  public void run(final GCloudCommandConfig config, final OutputStream outputStream) {
    final GCloudCommand gcloudCommand = this.gcloudCommandFactory
      .createWithOutputStream(checkNotNull(config), checkNotNull(outputStream));
    this.run(gcloudCommand);
  }

  private void run(final GCloudCommand gcloudCommand) {
    this.processConsumer.accept(gcloudCommand);
  }

}
