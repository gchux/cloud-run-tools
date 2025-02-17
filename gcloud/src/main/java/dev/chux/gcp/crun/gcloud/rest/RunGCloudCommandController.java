package dev.chux.gcp.crun.gcloud.rest;

import java.util.UUID;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.model.GCloudCommand;
import dev.chux.gcp.crun.gcloud.GCloudService;
import dev.chux.gcp.crun.rest.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;
import static com.google.common.base.Strings.isNullOrEmpty;

public class RunGCloudCommandController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunGCloudCommandController.class);

  private static final String SYS_OUT = "sys";

  public static final String KEY = RestModule.NAMESPACE + "/exec";

  private final Gson gson;
  private final GCloudService gcloudService;

  @Inject
  public RunGCloudCommandController(final Gson gson, final GCloudService gcloudService) {
    this.gson = gson;
    this.gcloudService = gcloudService;
  }

  public void register(final String root) {
    path(root, () -> {
      post("/exec", "*/*", this);
      path("/exec", () -> {
        post("/:namespace", "*/*", this);
      });
    });
  }

  public String endpoint(final String root) {
    return "POST " + root + "/exec[/optional:namespace]";
  }

  public Object handle(final Request request, final Response response) throws Exception {
    final String executionID = UUID.randomUUID().toString();

    final String rawJSON = request.body();
    final String namespace = request.params(":namespace");
    final String output = request.queryParams("output"); 
    
    final ServletOutputStream responseOutput = response.raw().getOutputStream();

    response.header("x-gcloud-execution-id", executionID);

    final Optional<GCloudCommand> maybeCmd = this.command(rawJSON);
    if (!maybeCmd.isPresent()) {
      halt(404, "command not found");
      return null;
    }

    final GCloudCommand cmd = maybeCmd.get();

    if (!isNullOrEmpty(namespace)) {
      // namespace is optional to allow for commands like `gcloud --help`
      response.header("x-gcloud-namespace", namespace);
      cmd.namespace(namespace);
    }

    logger.info("starting: {}", executionID);

    if( isNullOrEmpty(output) ) {
      this.gcloudService.run(cmd, responseOutput);
    } else {
      this.gcloudService.run(cmd);
    }
    
    logger.info("finished: {}", executionID);
    return null;
  }

  private final Optional<GCloudCommand> command(final String rawJSON) {
    try {
      final GCloudCommand cmd = this.gson.fromJson(rawJSON, GCloudCommand.class);
      return Optional.fromNullable(cmd);
    } catch(Exception ex) {
      ex.printStackTrace(System.err);
      logger.error("invalid 'gcloud' command: {}", rawJSON);
      logger.error("failed to parse json", ex);
    }
    return Optional.absent();
  }

}
