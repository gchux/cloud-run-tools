package dev.chux.gcp.crun.faults.bin;

import java.io.OutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;
import com.google.common.base.Optional;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

class Curl {

  private static abstract class AbstractCurl extends AbstractBinary<HttpRequest> {

    protected AbstractCurl(
      final ConfigService configService,
      final String binary
    ) {
      super(configService, "curl." + binary);
    }

    protected final ManagedProcessBuilder newCurlCommandBuilder(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) throws ManagedProcessException {
      final ManagedProcessBuilder builder = new ManagedProcessBuilder(get());
      setMethod(builder, request)
        .addArgument(builder, request.url())
        .addStdOut(builder, stdout)
        .addStdErr(builder, stderr);
      return builder;
    }

    protected final AbstractCurl setMethod(
      final ManagedProcessBuilder builder,
      final HttpRequest request
    ) {
      final Optional<String> method = request.optionalMethod();
      if (method.isPresent()) {
        addFlag(builder, "--request", method.get());
      }
      return this;
    }

  }

  static class Java extends AbstractCurl {

    @Inject
    public Java(
      final ConfigService configService
    ) {
      super(configService, "java");
    }

    @Override
    public ManagedProcessBuilder getBuilder(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) throws ManagedProcessException {
      return newCurlCommandBuilder(request, stdout, stderr);
    }

  }

  static class Python extends AbstractCurl {

    @Inject
    public Python(
      final ConfigService configService
    ) {
      super(configService, "python");
    }

    @Override
    public ManagedProcessBuilder getBuilder(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) throws ManagedProcessException {
      return newCurlCommandBuilder(request, stdout, stderr);
    }

  }

  static class NodeJS extends AbstractCurl {

    @Inject
    public NodeJS(
      final ConfigService configService
    ) {
      super(configService, "nodejs");
    }

    @Override
    public ManagedProcessBuilder getBuilder(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) throws ManagedProcessException {
      return newCurlCommandBuilder(request, stdout, stderr);
    }

  }

  static class Golang extends AbstractCurl {

    @Inject
    public Golang(
      final ConfigService configService
    ) {
      super(configService, "golang");
    }

    @Override
    public ManagedProcessBuilder getBuilder(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) throws ManagedProcessException {
      return newCurlCommandBuilder(request, stdout, stderr);
    }

  }

  static class Linux extends AbstractCurl {

    @Inject
    public Linux(
      final ConfigService configService
    ) {
      super(configService, "linux");
    }

    @Override
    public ManagedProcessBuilder getBuilder(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) throws ManagedProcessException {
      return newCurlCommandBuilder(request, stdout, stderr);
    }

  }

}
