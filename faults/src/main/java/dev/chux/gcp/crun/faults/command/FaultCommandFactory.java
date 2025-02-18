package dev.chux.gcp.crun.faults.command;

import java.io.OutputStream;

import javax.annotation.Nullable;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import com.google.common.base.Optional;

import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.model.GoogleAPIsRequest;

public interface FaultCommandFactory {

  // @Named("faults://commands/http/request")
  @Named(HttpRequestCommand.KEY)
  public FaultCommand<HttpRequest> newHttpRequestCommand(
    @Assisted("request") final HttpRequest request,
    @Assisted("runtime") final Optional<String> runtime,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://commands/googleapis/http/request")
  @Named(GoogleAPIsHttpRequestCommand.KEY)
  public FaultCommand<GoogleAPIsRequest> newGoogleAPIsHttpRequestCommand(
    @Assisted("request") final GoogleAPIsRequest request,
    @Assisted("runtime") final String runtime,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );
  
}
