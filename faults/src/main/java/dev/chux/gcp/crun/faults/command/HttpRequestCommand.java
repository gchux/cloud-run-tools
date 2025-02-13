package dev.chux.gcp.crun.faults.command;

import java.io.OutputStream;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;

import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcess;
import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.faults.bin.Binary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class HttpRequestCommand implements FaultCommand<HttpRequest> {

  private static final Logger logger = LoggerFactory.getLogger(HttpRequestCommand.class);

  private final Map<String, Binary<HttpRequest>> curlBinaries;
  private final HttpRequest httpRequest;
  private final Optional<OutputStream> stdout, stderr;

  @AssistedInject
  public HttpRequestCommand(
    @Named("faults://binaries/curl") Map<String, Binary<HttpRequest>> curlBinaries,
    @Assisted HttpRequest httpRequest,
    @Assisted("stdout") Optional<OutputStream> stdout,
    @Assisted("stderr") Optional<OutputStream> stderr
  ) {
    this.curlBinaries = curlBinaries;
    this.httpRequest = httpRequest;
    this.stdout = stdout;
    this.stderr = stderr;
  }

  @Override
  public HttpRequest get() {
    return this.httpRequest;
  }

  @Override
  public ManagedProcessBuilder getBuilder() throws ManagedProcessException {
    return this.curlBinaries.get("faults://binaries/curl/java")
      .getBuilder(this.httpRequest, this.stdout, this.stderr);
  } 

  @Override
  public void run(final HttpRequest request) {
    try {
      final ManagedProcess p = this.getBuilder()
        .setDestroyOnShutdown(true)
        .setConsoleBufferMaxLines(0)
        .build().start();
      p.waitForExit();
    } catch(final Exception ex) {
      logger.error(getStackTraceAsString(ex));
    }
  }

}
