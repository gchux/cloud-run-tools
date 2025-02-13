package dev.chux.gcp.crun.faults.command;

import java.io.OutputStream;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import com.google.common.base.Optional;

import dev.chux.gcp.crun.model.HttpRequest;

public interface FaultCommandFactory {

  @Named("faults://commands/httpRequest")
  public FaultCommand newHttpRequestCommand(
    final HttpRequest request,
    @Assisted("runtime") final Optional<String> runtime,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );
  
}
