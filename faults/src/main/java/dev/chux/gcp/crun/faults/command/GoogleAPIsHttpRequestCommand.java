package dev.chux.gcp.crun.faults.command;

import java.io.OutputStream;

import java.util.Collection;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

import dev.chux.gcp.crun.faults.binary.CurlFactory;

import dev.chux.gcp.crun.model.GoogleAPIsRequest;

import dev.chux.gcp.crun.process.ManagedProcessProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class GoogleAPIsHttpRequestCommand implements FaultCommand<GoogleAPIsRequest> {

  private static final Logger logger = LoggerFactory.getLogger(GoogleAPIsHttpRequestCommand.class);

  final static String KEY = CommandModule.NAMESPACE + "/googleapis/http/request";

  private final CurlFactory curlFactory;
  private final GoogleAPIsRequest request;
  private final String runtime;
  private final Optional<String> projectId;
  private final Optional<OutputStream> stdout, stderr;

  @AssistedInject
  public GoogleAPIsHttpRequestCommand(
    final CurlFactory curlFactory,
    @Assisted("request") final GoogleAPIsRequest request,
    @Assisted("runtime") final String runtime,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  ) {
    this.curlFactory = curlFactory;

    checkArgument(!isNullOrEmpty(runtime));
    this.runtime = runtime;
    
    this.request = request;
    this.stdout = stdout;
    this.stderr = stderr;

    this.projectId = request.optionalProjectId();
  }

  @Override
  public GoogleAPIsRequest get() {
    return this.request;
  }

  private final ManagedProcessProvider newCurlProvider() {
    final ManagedProcessProvider provider =
      this.curlFactory
        .newCurlWithGoogleAuthToken(
          this.runtime,
          this.projectId,
          this.request,
          this.stdout,
          this.stderr
        );
    logger.info("curl: {}", provider);
    return provider;
  }

  @Override
  public ManagedProcessBuilder getBuilder() throws ManagedProcessException {
    return this.newCurlProvider().getBuilder();
  }

  @Override
  public Collection<ManagedProcessProvider> getProviders() {
    return ImmutableList.of(this.newCurlProvider());
  } 

}
