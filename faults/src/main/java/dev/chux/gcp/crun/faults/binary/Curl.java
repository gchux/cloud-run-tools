package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.model.HttpProxy;
import dev.chux.gcp.crun.model.HttpRequest;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

public class Curl {

  private static final Joiner HTTP_HEADER_JOINER = Joiner.on(": ").useForNull("ignored");

  static final String newHttpHeader(final String name, final String value) {
    return  HTTP_HEADER_JOINER.join(name, value);
  }

  static final String newHttpHeader(final Map.Entry<String, String> header) {
    return Curl.newHttpHeader(header.getKey(), header.getValue());
  }

  public static final String runtimeKey(final Optional<String> runtime) {
    if (!runtime.isPresent()) {
      return Linux.KEY;
    }
    return runtimeKey(runtime.get());
  }

  static final String runtimeKey(final String runtime) {
    if (isNullOrEmpty(runtime)) {
      return Linux.KEY;
    }

    final String rt = runtime.toLowerCase();
    
    switch(rt) {
      case Java.BINARY:
        return Java.KEY;
      case Python.BINARY:
        return Python.KEY;
      case NodeJS.BINARY:
        return NodeJS.KEY;
      case Golang.BINARY:
        return Golang.KEY;
      case Linux.BINARY:
      default:
        return Linux.KEY;
    }
  }

  public static class Java extends AbstractCurl {

    public static final String KEY = CurlModule.NAMESPACE + "/java";

    public static final String BINARY = "java";

    @Inject
    public Java(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public Java(
      final ConfigService configService,
      @Assisted("request") HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

    private final String getProxyHost(
      final String protocol,
      final HttpProxy proxy
    ) {
      return "-D" + protocol + ".proxyHost=" + proxy.host();
    }

    private final String getProxyPort(
      final String protocol,
      final HttpProxy proxy
    ) {
      final String port =  Integer.toString(proxy.port(), 10);
      return "-D" + protocol + ".proxyPort=" + port;
    }

    private final String getHttpProxyHost(
      final HttpProxy proxy
    ) {
      return this.getProxyHost("http", proxy);
    }

    private final String getHttpsProxyHost(
      final HttpProxy proxy
    ) {
      return this.getProxyHost("https", proxy);
    }

    private final String getHttpProxyPort(
      final HttpProxy proxy
    ) {
      return this.getProxyPort("http", proxy);
    }

    private final String getHttpsProxyPort(
      final HttpProxy proxy
    ) {
      return this.getProxyPort("https", proxy);
    }

    private final String getProxyJavaToolOption(
      final String httpProxyHost,
      final String httpProxyPort,
      final String httpsProxyHost,
      final String httpsProxyPort
    ) {
      final String httpProxy = httpProxyHost + " " + httpProxyPort;
      final String httpsProxy = httpsProxyHost + " " + httpsProxyPort;
      return httpProxy + " " + httpsProxy;
    }

    private final void setJavaToolOptions(
      final ManagedProcessBuilder builder,
      final String javaToolOptions
    ) {
      setEnvVar(builder, "JAVA_TOOL_OPTIONS", javaToolOptions);
    }

    @Override
    protected AbstractCurl setProxy(
      final ManagedProcessBuilder builder,
      final HttpProxy proxy
    ) {
      final String httpProxyHost = this.getHttpProxyHost(proxy);
      final String httpProxyPort = this.getHttpProxyPort(proxy);

      final String httpsProxyHost = this.getHttpsProxyHost(proxy);
      final String httpsProxyPort = this.getHttpsProxyPort(proxy);

      this.setJavaToolOptions(builder, this.getProxyJavaToolOption(
        httpProxyHost, httpProxyPort, httpsProxyHost, httpsProxyPort));

      return super.setProxy(builder, proxy);
    }

  }

  public static class Python extends AbstractCurl {

    public static final String KEY = CurlModule.NAMESPACE + "/python";

    public static final String BINARY = "python";

    @Inject
    public Python(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public Python(
      final ConfigService configService,
      @Assisted("request") HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

  }

  public static class NodeJS extends AbstractCurl {

    public static final String KEY = CurlModule.NAMESPACE + "/nodejs";

    public static final String BINARY = "nodejs";

    @Inject
    public NodeJS(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public NodeJS(
      final ConfigService configService,
      @Assisted("request") HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

  }

  public static class Golang extends AbstractCurl {

    public static final String KEY = CurlModule.NAMESPACE + "/golang";

    public static final String BINARY = "golang";

    @Inject
    public Golang(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public Golang(
      final ConfigService configService,
      @Assisted("request") HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

  }

  public static class Linux extends AbstractCurl {

    public static final String KEY = CurlModule.NAMESPACE + "/linux";

    public static final String BINARY = "linux";

    @Inject
    public Linux(
      final ConfigService configService
    ) {
      super(configService, BINARY);
    }

    @AssistedInject
    public Linux(
      final ConfigService configService,
      @Assisted("request") HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, BINARY, request, stdout, stderr);
    }

    @Override
    protected AbstractCurl setMethod(
      final ManagedProcessBuilder builder,
      final String method
    ) {
      builder
        .addArgument("--request")
        .addArgument(method);
      return this;
    }

    @Override
    protected AbstractCurl setHeader(
      final ManagedProcessBuilder builder,
      final String name, final String value
    ) {
      builder
        .addArgument("--header")
        .addArgument(Curl.newHttpHeader(name, value), false);
      return this;
    }

    @Override
    protected AbstractCurl setData(
      final ManagedProcessBuilder builder,
      final String data
    ) {
      builder
        .addArgument("--data-raw")
        .addArgument(data);
      return this;
    }

    private final String getProxyEndpoint(
      final HttpProxy proxy
    ) {
      return proxy.host() + ":" + Integer.toString(proxy.port(), 10);
    }

    @Override
    protected AbstractCurl setProxy(
      final ManagedProcessBuilder builder,
      final HttpProxy proxy
    ) {
      // see: https://curl.se/docs/manpage.html#-x
      builder.addArgument("--proxy")
        .addArgument(this.getProxyEndpoint(proxy));
      return super.setProxy(builder, proxy);
    }

  }

  static abstract class WithGoogleAuthorization extends AbstractCurl {

    public static final String KEY = CurlModule.NAMESPACE + "/google";

    private final CurlFactory curlFactory;
    private final String runtime;
    private final String runtimeKey;
    private final Optional<String> projectId;

    private Optional<AbstractCurl> delegate;

    public WithGoogleAuthorization(
      final ConfigService configService,
      final CurlFactory curlFactory,
      final String binary
    ) {
      super(configService, "google." + binary);

      this.curlFactory = curlFactory;

      this.runtime = Linux.BINARY;
      this.runtimeKey = Linux.KEY;
      
      this.projectId = absent();
      this.delegate = absent();
    }

    public WithGoogleAuthorization(
      final ConfigService configService,
      final CurlFactory curlFactory,
      final String binary,
      /* assisted */ final String runtime,
      /* assisted */ final Optional<String> projectId,
      /* assisted */ final HttpRequest request,
      /* assisted */ final Optional<OutputStream> stdout,
      /* assisted */ final Optional<OutputStream> stderr
    ) {
      super(configService, "google." + binary, request, stdout, stderr);

      checkArgument(!isNullOrEmpty(runtime));
      this.runtime = runtime;
      this.runtimeKey = Curl.runtimeKey(runtime);

      this.curlFactory = curlFactory;

      this.projectId = projectId;

      this.delegate = Optional.of(this.newCurl(request, stdout, stderr));
    }

    private final AbstractCurl newCurl(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) {
      checkNotNull(request);

      switch(this.runtimeKey) {
        case Curl.Java.KEY:
          return this.curlFactory.newCurlJava(request, stdout, stderr);
        case Curl.Python.KEY:
          return this.curlFactory.newCurlPython(request, stdout, stderr);
        case Curl.Golang.KEY:
          return this.curlFactory.newCurlGolang(request, stdout, stderr);
        case Curl.NodeJS.KEY:
          return this.curlFactory.newCurlNodeJS(request, stdout, stderr);
        case Curl.Linux.KEY:
        default:
          return this.curlFactory.newCurlLinux(request, stdout, stderr);
      }
    }

    private final AbstractCurl delegate() {
      return this.delegate.get();
    }

    private final AbstractCurl setDelegate(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) {
      final AbstractCurl delegate = this.newCurl(request, stdout, stderr);
      this.delegate = Optional.of(delegate);
      return delegate;
    }

    private final void resetDelegate() {
      this.delegate = absent();
    }

    @Override
    public String get() {
      return this.runtime;
    }

    @Override
    protected AbstractCurl setMethod(
      final ManagedProcessBuilder builder,
      final String method
    ) {
      return this.delegate().setMethod(builder, method);
    }

    @Override
    protected AbstractCurl setHeader(
      final ManagedProcessBuilder builder,
      final String name, final String value
    ) {
      if (this.projectId.isPresent()) {
        this.delegate()
          .setHeader(builder,
            "X-Goog-User-Project",
            this.projectId.get()
          );
      }
      return this.delegate().setHeader(builder, name, value);
    }

    @Override
    protected AbstractCurl setData(
      final ManagedProcessBuilder builder,
      final String data
    ) {
      return this.delegate().setData(builder, data);
    }

    @Override
    protected AbstractCurl setProxy(
      final ManagedProcessBuilder builder,
      final HttpProxy proxy
    ) {
      return this.delegate().setProxy(builder, proxy);
    }

    @Override
    protected AbstractBinary<HttpRequest> setEnvironment(
      final ManagedProcessBuilder builder
    ) {
      this.delegate().setEnvVar(builder,
        "X_FLAG_SEPARATOR",
        this.delegate().flagSeparator()
      );
      return super.setEnvironment(builder);
    }

    /**
     * `delegate` is mutable state, thus to be thread-safe,
     * `getBuilder(request, stdout, stderr)` must be `synchronized`.
     */
    @Override
    public synchronized ManagedProcessBuilder getBuilder(
      final HttpRequest request,
      final Optional<OutputStream> stdout,
      final Optional<OutputStream> stderr
    ) throws ManagedProcessException {
      checkNotNull(request);
      final AbstractCurl delegate = this.setDelegate(request, stdout, stderr);
      final ManagedProcessBuilder builder = super.getBuilder(request, stdout, stderr);
      this.resetDelegate();
      return builder;
    }

    @Override
    public String toString() {
      return toStringHelper(this)
      .add("id", id())
      .add("binary", binary())
      .add("runtime", this.runtime)
      .add("runtime_key", this.runtimeKey)
      .add("project_id", this.projectId)
      .add("delegate", this.delegate)
      .toString();
    }

  }

  public static class WithGoogleIdToken extends WithGoogleAuthorization {

    public static final String KEY = WithGoogleAuthorization.KEY + "/id";

    private static final String BINARY = "id";

    @Inject
    public WithGoogleIdToken(
      final ConfigService configService,
      final CurlFactory curlFactory
    ) {
      super(configService, curlFactory, BINARY);
    }

    @AssistedInject
    public WithGoogleIdToken(
      final ConfigService configService,
      final CurlFactory curlFactory,
      @Assisted("runtime") String runtime,
      @Assisted("projectId") Optional<String> projectId,
      @Assisted("request") HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, curlFactory, BINARY, runtime, projectId, request, stdout, stderr);
    }

  }

  public static class WithGoogleAuthToken extends WithGoogleAuthorization {

    public static final String KEY = WithGoogleAuthorization.KEY + "/auth";

    private static final String BINARY = "auth";

    @Inject
    public WithGoogleAuthToken(
      final ConfigService configService,
      final CurlFactory curlFactory
    ) {
      super(configService, curlFactory, BINARY);
    }

    @AssistedInject
    public WithGoogleAuthToken(
      final ConfigService configService,
      final CurlFactory curlFactory,
      @Assisted("runtime") String runtime,
      @Assisted("projectId") Optional<String> projectId,
      @Assisted("request") HttpRequest request,
      @Assisted("stdout") Optional<OutputStream> stdout,
      @Assisted("stderr") Optional<OutputStream> stderr
    ) {
      super(configService, curlFactory, BINARY, runtime, projectId, request, stdout, stderr);
    }

  }

}
