package dev.chux.gcp.crun.process;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

public interface ManagedProcessProvider {

  public ManagedProcessBuilder getBuilder() throws ManagedProcessException;

}
