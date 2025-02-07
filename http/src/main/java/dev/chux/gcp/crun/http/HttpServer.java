package dev.chux.gcp.crun.http;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.netflix.governator.annotations.Configuration;

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;

import dev.chux.gcp.crun.ConfigService;
import dev.chux.gcp.crun.rest.RestAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {
  private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

  private static final int DEFAULT_SERVER_PORT = 8080;

  private final RestAPI restAPI;
  private final Optional<Integer> serverPortEnv;
  private final Optional<Integer> serverPortProp;

  @Configuration("env.Port")
  private int _serverPortEnv;

  @Configuration("server.port")
  private int _serverPortProp;

  @Inject
  HttpServer(final ConfigService configService, final RestAPI restAPI) {
    this.restAPI = restAPI;
    this.serverPortEnv = this.getServerPortEnv(configService);
    this.serverPortProp = this.getServerPortProp(configService);
  }
  
  public void start() {
    final int serverPort = this.getServerPort();
    logger.info("serving at port: [env:{}][prop:{}][default:{}]  => {}",
      this.serverPortEnv, this.serverPortProp, DEFAULT_SERVER_PORT, serverPort);
    this.restAPI.serve(serverPort);
  }

  private final int getServerPort() {
    return this.serverPortEnv.or(this.serverPortProp).or(DEFAULT_SERVER_PORT);
  }

  private final Optional<Integer> getServerPortEnv(final ConfigService configService) {
    return configService.getIntEnvVar("env.PORT");
  }

  private final Optional<Integer> getServerPortProp(final ConfigService configService) {
    return configService.getIntAppProp("server.port");
  }

}
