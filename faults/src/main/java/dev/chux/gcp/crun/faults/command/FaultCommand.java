package dev.chux.gcp.crun.faults.command;

import com.google.common.base.Supplier;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

import dev.chux.gcp.crun.process.ManagedProcessProvider;

public interface FaultCommand<T> extends ManagedProcessProvider, Supplier<T> {

  public void run(final T input);

}
