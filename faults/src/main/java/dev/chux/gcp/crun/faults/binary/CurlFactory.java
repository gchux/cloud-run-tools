package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import com.google.common.base.Optional;

import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

public interface CurlFactory {

  // @Named("faults://binaries/curl/linux")
  @Named(Curl.Linux.KEY)
  public ManagedProcessProvider newCurlLinux(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/java")
  @Named(Curl.Java.KEY)
  public ManagedProcessProvider newCurlJava(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/python")
  @Named(Curl.Python.KEY)
  public ManagedProcessProvider newCurlPython(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/nodejs")
  @Named(Curl.NodeJS.KEY)
  public ManagedProcessProvider newCurlNodeJS(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/golang")
  @Named(Curl.Golang.KEY)
  public ManagedProcessProvider newCurlGolang(
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/google/id")
  @Named(Curl.WithGoogleIdToken.KEY)
  public ManagedProcessProvider newCurlWithGoogleIdToken(
    @Assisted("runtime") final String runtime,
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/google/auth")
  @Named(Curl.WithGoogleAuthToken.KEY)
  public ManagedProcessProvider newCurlWithGoogleAuthToken(
    @Assisted("runtime") final String runtime,
    @Assisted("request") final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );
  
}
