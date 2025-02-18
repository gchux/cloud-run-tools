package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.model.HttpProxy;
import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;

abstract class AbstractCurl 
  extends AbstractBinary<HttpRequest>
  implements ManagedProcessProvider {

  private final Optional<HttpRequest> request;
  private final Optional<OutputStream> stdout;
  private final Optional<OutputStream> stderr;

  protected final String binary;

  protected AbstractCurl(
    final ConfigService configService,
    final String binary
  ) {
    this(configService, binary, null, absent(), absent());
  }

  protected AbstractCurl(
    final ConfigService configService,
    final String binary,
    @Nullable final HttpRequest request,
    Optional<OutputStream> stdout,
    Optional<OutputStream> stderr
  ) {
    super(configService, "curl." + binary);
    this.binary = binary;
    this.request = fromNullable(request);
    this.stdout = stdout;
    this.stderr = stderr;
  }

  private final ManagedProcessBuilder newCurlCommandBuilder(
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

  private final AbstractCurl setMethod(
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

  private final AbstractCurl setData(
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

  private final AbstractCurl setHeaders(
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

  private final String getHttpsProxyProtocol(
    final HttpProxy proxy
  ) {
    return (proxy.isSecure()? "https" : "http") + "://";
  }

  private final String getProxyHostAndPort(
    final HttpProxy proxy
  ) {
    final String port =  Integer.toString(proxy.port(), 10);
    return proxy.host() + ":" + port;
  }

  private AbstractCurl setProxy(
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

    setEnvVar(builder, "X_HTTP_PROXY", httpProxy);
    setEnvVar(builder, "X_HTTPS_PROXY", httpProxy);

    return this;
  }

  @Override
  protected AbstractBinary<HttpRequest> setEnvironment(
    final ManagedProcessBuilder builder
  ) {
    return setEnvVar(builder, "X_CURL_RUNTIME", get())
      .setEnvVar(builder, "X_CURL_BINARY", binary());
  }

  @Override
  public ManagedProcessBuilder getBuilder(
    final HttpRequest request,
    final Optional<OutputStream> stdout,
    final Optional<OutputStream> stderr
  ) throws ManagedProcessException {
    checkNotNull(request);
    return newCurlCommandBuilder(request, stdout, stderr);
  }

  @Override
  public ManagedProcessBuilder getBuilder() throws ManagedProcessException {
    return this.getBuilder(this.request.orNull(), this.stdout, this.stderr);
  }

  @Override
  public String get() {
    return this.binary;
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .add("id", id())
      .add("binary", binary())
      .add("flag_separator", flagSeparator())
      .addValue(this.request)
      .toString();
  }

}
