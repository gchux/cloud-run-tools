package dev.chux.gcp.crun.faults.socket;

import java.net.ServerSocket;

import com.google.common.base.Optional;

public interface ServerSocketsProvider {

  public Optional<ServerSocket> get(final String socketName);

}
