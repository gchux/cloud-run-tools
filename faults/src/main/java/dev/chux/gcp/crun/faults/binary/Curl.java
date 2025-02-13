package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;

import com.google.inject.Inject;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;

import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;
import com.google.common.base.Optional;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

class Curl {

  static abstract class AbstractCurl 
    extends AbstractBinary<HttpRequest>
    implements ManagedProcessProvider {

    private final Optional<HttpRequest> request;
    private final Optional<OutputStream> stdout;
    private final Optional<OutputStream> stderr;

    protected AbstractCurl(
      final ConfigService configService,
      final String binary
    ) {
      this(configService, binary, null, absent(), absent());
    }

    protected AbstractCurl(
      final ConfigService configService,
      final String binary,
       HttpRequest request,
       Optional<OutputStream> stdout,
       Optional<OutputStream> stderr
    ) {
      super(configService, "curl." + binary);
      this.request = fromNullable(request);
      this.stdout = stdout;
      this.stderr = stderr;
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

    @Override
    public ManagedProcessBuilder getBuilder(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) throws ManagedProcessException {
      return newCurlCommandBuilder(request, stdout, stderr);
    }

    @Override
    public ManagedProcessBuilder getBuilder() throws ManagedProcessException {
      return this.getBuilder(this.request.get(), this.stdout, this.stderr);
    }

  }

  static class Java extends AbstractCurl {

    public static final String NAMESPACE = CurlModule.NAMESPACE + "/java";

    private static final String BINARY = "java";

    @Inject
    public Java(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public Java(
      final ConfigService configService,
      @Assisted HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

  }

  static class Python extends AbstractCurl {

    public static final String NAMESPACE = CurlModule.NAMESPACE + "/python";

    private static final String BINARY = "python";

    @Inject
    public Python(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public Python(
      final ConfigService configService,
      @Assisted HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

  }

  static class NodeJS extends AbstractCurl {

    public static final String NAMESPACE = CurlModule.NAMESPACE + "/nodejs";

    private static final String BINARY = "nodejs";

    @Inject
    public NodeJS(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public NodeJS(
      final ConfigService configService,
      @Assisted HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

  }

  static class Golang extends AbstractCurl {

    public static final String NAMESPACE = CurlModule.NAMESPACE + "/golang";

    private static final String BINARY = "golang";

    @Inject
    public Golang(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public Golang(
      final ConfigService configService,
      final String binary,
      @Assisted HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

  }

  static class Linux extends AbstractCurl {

    public static final String NAMESPACE = CurlModule.NAMESPACE + "/linux";

    private static final String BINARY = "linux";

    @Inject
    public Linux(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public Linux(
      final ConfigService configService,
      final String binary,
      @Assisted HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

  }

}
