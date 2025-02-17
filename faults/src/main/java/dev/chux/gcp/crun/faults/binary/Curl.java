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
import com.google.common.base.Optional;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.model.HttpProxy;
import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

class Curl {

  private static final Joiner HTTP_HEADER_JOINER = Joiner.on(": ").useForNull("ignored");

  private static final String newHttpHeader(final String name, final String value) {
    return  HTTP_HEADER_JOINER.join(name, value);
  }

  private static final String newHttpHeader(final Map.Entry<String, String> header) {
    return Curl.newHttpHeader(header.getKey(), header.getValue());
  }

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
      final ManagedProcessBuilder builder = newCommandBuilder();
      setMethod(builder, request)
        .setHeaders(builder, request)
        .setProxy(builder, request)
        .setData(builder, request)
        // methods from `AbstractBinary`
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
        this.setMethod(builder, method.get());
      }
      return this;
    }

    protected AbstractCurl setMethod(
      final ManagedProcessBuilder builder,
      final String method
    ) {
      super.addLongFlag(builder, "request", method);
      return this;
    }

    protected final AbstractCurl setData(
      final ManagedProcessBuilder builder,
      final HttpRequest request
    ) {
      final Optional<String> data = request.optionalData();
      if (data.isPresent()) {
        this.setData(builder, data.get());
      }
      return this;
    }

    protected AbstractCurl setData(
      final ManagedProcessBuilder builder,
      final String data
    ) {
      super.addLongFlag(builder, "data-raw", data);
      return this;
    }

    protected final AbstractCurl setHeaders(
      final ManagedProcessBuilder builder,
      final HttpRequest request
    ) {
      final Map<String, String> headers = request.headers();
      for(final Map.Entry<String, String> header : headers.entrySet()) {
        this.setHeader(builder, header.getKey(), header.getValue());
      }
      return this;
    }

    protected AbstractCurl setHeader(
      final ManagedProcessBuilder builder,
      final String name, final String value
    ) {
      super.addLongFlag(builder, "header", Curl.newHttpHeader(name, value));
      return this;
    }

    protected final String getHttpsProxyProtocol(
      final HttpProxy proxy
    ) {
      return (proxy.isSecure()? "https" : "http") + "://";
    }

    protected final String getProxyHostAndPort(
      final HttpProxy proxy
    ) {
      final String port =  Integer.toString(proxy.port(), 10);
      return proxy.host() + ":" + port;
    }

    protected AbstractCurl setProxy(
      final ManagedProcessBuilder builder,
      final HttpRequest request
    ) {
      final Optional<HttpProxy> proxy = request.optionalProxy();
      if (proxy.isPresent()) {
        return setProxy(builder, proxy.get());
      }
      return this;
    }

    protected AbstractCurl setProxy(
      final ManagedProcessBuilder builder,
      final HttpProxy proxy
    ) {
      final String hostAndPort = this.getProxyHostAndPort(proxy);
      final String httpProxy = "http://" + hostAndPort;
      final String httpsProxy = this.getHttpsProxyProtocol(proxy) + hostAndPort;

      setEnvVar(builder, "http_proxy", httpProxy);
      setEnvVar(builder, "https_proxy", httpsProxy);

      setEnvVar(builder, "HTTP_PROXY", httpProxy);
      setEnvVar(builder, "HTTPS_PROXY", httpsProxy);

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

    public static final String KEY = CurlModule.NAMESPACE + "/java";

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

  static class Python extends AbstractCurl {

    public static final String KEY = CurlModule.NAMESPACE + "/python";

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

    public static final String KEY = CurlModule.NAMESPACE + "/nodejs";

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

    public static final String KEY = CurlModule.NAMESPACE + "/golang";

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

    public static final String KEY = CurlModule.NAMESPACE + "/linux";

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

}
