package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import com.google.common.base.Optional;

import dev.chux.gcp.crun.model.HttpRequest;
import dev.chux.gcp.crun.process.ManagedProcessProvider;

public interface CurlFactory {

  // @Named("faults://binaries/curl/linux")
  @Named(Curl.Linux.NAMESPACE)
  public ManagedProcessProvider newCurlLinux(
    final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/java")
  @Named(Curl.Java.NAMESPACE)
  public ManagedProcessProvider newCurlJava(
    final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/python")
  @Named(Curl.Python.NAMESPACE)
  public ManagedProcessProvider newCurlPython(
    final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/nodejs")
  @Named(Curl.NodeJS.NAMESPACE)
  public ManagedProcessProvider newCurlNodeJS(
    final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );

  // @Named("faults://binaries/curl/golang")
  @Named(Curl.Golang.NAMESPACE)
  public ManagedProcessProvider newCurlGolang(
    final HttpRequest request,
    @Assisted("stdout") final Optional<OutputStream> stdout,
    @Assisted("stderr") final Optional<OutputStream> stderr
  );
  
}
