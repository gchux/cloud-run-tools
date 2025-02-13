package dev.chux.gcp.crun.faults;

import java.io.OutputStream;

import java.util.function.Consumer;

import com.google.inject.Inject;

import com.google.common.base.Optional;

import dev.chux.gcp.crun.faults.command.FaultCommand;
import dev.chux.gcp.crun.faults.command.FaultCommandFactory;
import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

import static com.google.common.base.Preconditions.checkNotNull;

public class FaultsService {

  private final FaultCommandFactory faultCommandFactory;
  private final Consumer<ManagedProcessProvider> processConsumer;
  
  @Inject
  FaultsService(FaultCommandFactory faultCommandFactory,
      @ProcessConsumer Consumer<ManagedProcessProvider> processConsumer) {
    this.faultCommandFactory = faultCommandFactory;
    this.processConsumer = processConsumer;
  }

  public void runHttpRequest(
    final HttpRequest request,
    final Optional<OutputStream> stdout,
    final Optional<OutputStream> stderr
  ) {
    final FaultCommand command = this.faultCommandFactory
      .newHttpRequestCommand(request, stdout, stderr);
    this.run(command);
  }

  private final void run(final FaultCommand command) {
    this.processConsumer.accept(command);
  }

}
