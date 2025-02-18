package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import com.google.common.base.Optional;

import dev.chux.gcp.crun.model.HttpRequest;

public interface CurlFactory {

  // @Named("faults://binaries/curl/linux")
  @Named(Curl.Linux.KEY)
  public Curl.Linux newCurlLinux(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/java")
  @Named(Curl.Java.KEY)
  public Curl.Java newCurlJava(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/python")
  @Named(Curl.Python.KEY)
  public Curl.Python newCurlPython(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/nodejs")
  @Named(Curl.NodeJS.KEY)
  public Curl.NodeJS newCurlNodeJS(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/golang")
  @Named(Curl.Golang.KEY)
  public Curl.Golang newCurlGolang(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/google/id")
  @Named(Curl.WithGoogleIdToken.KEY)
  public Curl.WithGoogleIdToken newCurlWithGoogleIdToken(
    @Assisted("runtime") final String runtime,
    @Assisted("projectId") final Optional<String> projectId,
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/google/auth")
  @Named(Curl.WithGoogleAuthToken.KEY)
  public Curl.WithGoogleAuthToken newCurlWithGoogleAuthToken(
    @Assisted("runtime") final String runtime,
    @Assisted("projectId") final Optional<String> projectId,
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );
  
}
