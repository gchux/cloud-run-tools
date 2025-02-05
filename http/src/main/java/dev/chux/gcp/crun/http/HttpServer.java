package dev.chux.gcp.crun.http;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;

import dev.chux.gcp.crun.rest.RestAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {
  private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

  private static final int DEFAULT_SERVER_PORT = 8080;

  private final RestAPI restAPI;

  @Inject(optional=true)
  @Named("env.PORT")
  String serverPortEnv = null;

  @Inject(optional=true)
  @Named("server.port")
  String serverPortProp = null;

  @Inject
  HttpServer(final RestAPI restAPI) {
    this.restAPI = restAPI;
  }
  
  public void start() {
    final int serverPort = this.getServerPort();
    logger.info("serving at port: [env:{}][prop:{}][default:{}]  => {}",
      this.serverPortEnv, this.serverPortProp, DEFAULT_SERVER_PORT, serverPort);
    this.restAPI.serve(serverPort);
  }

  private int getServerPort() {
    final Optional<Integer> serverPortEnv = this.parseServerPort(this.serverPortEnv);
    final Optional<Integer> serverPortProp = this.parseServerPort(this.serverPortProp);
    return serverPortEnv.or(serverPortProp).or(DEFAULT_SERVER_PORT);
  }

  private Optional<Integer> parseServerPort(final String serverPort) {
    if (serverPort == null) {
      return Optional.absent();
    }
    return Optional.fromNullable(Ints.tryParse(serverPort));
  }

}
