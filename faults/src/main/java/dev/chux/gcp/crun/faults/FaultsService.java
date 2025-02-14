package dev.chux.gcp.crun.faults;

import java.io.OutputStream;

import java.util.function.Consumer;

import com.google.inject.Inject;

import com.google.common.base.Optional;

import dev.chux.gcp.crun.faults.command.FaultCommand;
import dev.chux.gcp.crun.faults.command.FaultCommandFactory;
import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ManagedProcessProvider;
import dev.chux.gcp.crun.process.ManagedMultiProcessProvider;
import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;
import dev.chux.gcp.crun.process.ProcessModule.MultiProcessConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class FaultsService {

  private final FaultCommandFactory faultCommandFactory;
  private final Consumer<ManagedProcessProvider> processConsumer;
  private final Consumer<ManagedMultiProcessProvider> multiProcessConsumer;
  
  @Inject
  FaultsService(
    FaultCommandFactory faultCommandFactory,
    @ProcessConsumer Consumer<ManagedProcessProvider> processConsumer,
    @MultiProcessConsumer Consumer<ManagedMultiProcessProvider> multiProcessConsumer) {
    this.faultCommandFactory = faultCommandFactory;
    this.processConsumer = processConsumer;
    this.multiProcessConsumer = multiProcessConsumer;
  }

  public void runHttpRequest(
    final HttpRequest request,
    final Optional<String> runtime,
    final Optional<OutputStream> stdout,
    final Optional<OutputStream> stderr
  ) {
    final FaultCommand command = this.faultCommandFactory
      .newHttpRequestCommand(request, runtime, stdout, stderr);

    if (runtime.isPresent()) {
      this.run(command);
    } else {
      this.runAll(command);
    }
  }

  private final void run(final FaultCommand command) {
    this.processConsumer.accept(command);
  }

  private final void runAll(final FaultCommand command) {
    this.multiProcessConsumer.accept(command);
  }

}
