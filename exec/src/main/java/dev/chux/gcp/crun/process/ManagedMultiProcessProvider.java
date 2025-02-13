package dev.chux.gcp.crun.process;

import java.util.Collection;

public interface ManagedMultiProcessProvider {

  public Collection<ManagedProcessProvider> getProviders();

}
