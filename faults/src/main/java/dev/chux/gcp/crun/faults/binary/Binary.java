package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;

import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

public interface Binary<T> {

  public ManagedProcessBuilder getBuilder(
    final T input,
    final Optional<OutputStream> stdout,
    final Optional<OutputStream> stderr
  ) throws ManagedProcessException;

}
