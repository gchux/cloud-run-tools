package dev.chux.gcp.crun.faults.socket;

import java.util.Map;
import java.util.Set;
import java.net.ServerSocket;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.google.common.base.Supplier;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import com.netflix.governator.annotations.Configuration;

import dev.chux.gcp.crun.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class ServerSocketsProviderImpl implements ServerSocketsProvider {
  private static final Logger logger = LoggerFactory.getLogger(ServerSocketsProviderImpl.class);

  private final Map<String, ServerSocket> serverSockets;

  @Inject
  public ServerSocketsProviderImpl(final ConfigService configService,
    @Named("socket-faults://names") final Set<String> socketNames) {
    this.serverSockets = this.createServerSockets(configService, socketNames);
    checkState(this.serverSockets.size() == socketNames.size(), "incimplete sockets");
  }

  private Map<String, ServerSocket> createServerSockets(final ConfigService configService, final Set<String> socketNames) {
    final ImmutableMap.Builder<String, ServerSocket> sockets = ImmutableMap.<String, ServerSocket>builder();
    for(final String socketName : socketNames) {
      final Optional<Integer> port = this.getSocketPort(configService, socketName);
      checkState(port.isPresent(), "undefined port for socket: ", socketName);
      this.addSocket(sockets, socketName, port.get());
    }
    return sockets.build();
  }

  private Optional<Integer> getSocketPort(final ConfigService configService, final String socketName) {
    return configService.getIntAppProp("faults.socket." + socketName + ".port");
  }

  private void addSocket(final ImmutableMap.Builder<String, ServerSocket> sockets, final String name, final Integer port) {
    try {
      sockets.put(name, new ServerSocket(port.intValue()));
      logger.info("created server socket '{}' at port '{}'", name, port);
    } catch(final Exception ex) {
      logger.error("failed to create server socket '{}' at port '{}': {}", name, port, getStackTraceAsString(ex));
    }
  }

  @Override
  public Optional<ServerSocket> get(final String socketName) {
    return fromNullable(this.serverSockets.get(socketName));
  }

}
