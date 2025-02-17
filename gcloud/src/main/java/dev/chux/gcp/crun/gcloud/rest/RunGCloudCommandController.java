package dev.chux.gcp.crun.gcloud.rest;

import java.io.OutputStreamWriter;

import java.util.List;
import java.util.UUID;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Optional;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.model.GCloudCommand;
import dev.chux.gcp.crun.model.GCloudCommands;
import dev.chux.gcp.crun.gcloud.GCloudService;
import dev.chux.gcp.crun.rest.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import static java.nio.charset.StandardCharsets.UTF_8;

import static java.util.Collections.singletonList;

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
    final String namespace = emptyToNull(request.params(":namespace"));

    response.header("x-gcloud-execution-id", executionID);

    logger.info("starting: {}", executionID);

    final Optional<GCloudCommands> commands;
    if ( isNullOrEmpty(namespace) || !namespace.equalsIgnoreCase("all") ) {
      commands = toGCloudCommands(
        fromNullable(namespace), this.commandPayload(rawJSON)
      );
    } else {
      commands = this.commandsPayload(rawJSON);
    }

    if ( commands.isPresent() ) {
      this.runCommands(request, response, commands.get());
    } else {
      halt(404, "command(s) not found");
    }
    
    logger.info("finished: {}", executionID);

    return null;
  }

  private final Optional<GCloudCommands> toGCloudCommands(
    final Optional<String> namespace,
    final Optional<GCloudCommand> command
  ) {
    if ( !command.isPresent() ) { return absent(); }
    return Optional.of(
      this.newGCloudCommands(singletonList(
        this.setNamespace(namespace, command.get())
      ))
    );
  }

  private final GCloudCommand setNamespace(
    final Optional<String> namespace,
    final GCloudCommand command
  ) {
    checkNotNull(command);
    if ( namespace.isPresent() ) {
      command.namespace(namespace.get());
    }
    return command;
  }

  private final GCloudCommands newGCloudCommands(
    final List<GCloudCommand> commands
  ) {
    return new GCloudCommands(commands);
  }

  private final void runCommand(
    final Request request,
    final Response response,
    final GCloudCommand command
  )  throws Exception {
    checkNotNull(command);

    logger.info("running GCloud Command: {}", command);

    final String output = request.queryParams("output"); 

    if( isNullOrEmpty(output) ) {
      this.gcloudService.run(command, response.raw().getOutputStream());
    } else {
      this.gcloudService.run(command);
    }
  }

  private final void runCommands(
    final Request request,
    final Response response,
    final GCloudCommands commands
  )  throws Exception {
    checkNotNull(commands);

    if ( commands.get().isEmpty() ) {
      // see: https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/204
      halt(204, "no commands");
      return;
    }

    final OutputStreamWriter writer =
      new OutputStreamWriter(response.raw().getOutputStream(), UTF_8);

    for (final GCloudCommand command : commands.get()) {
      this.runCommand(request, response, command);
      writer.write("\n---\n");
      writer.flush();
    }
  }

  private final Optional<GCloudCommand> commandPayload(final String rawJSON) {
    return this.payload(rawJSON, GCloudCommand.class);
  }

  private final Optional<GCloudCommands> commandsPayload(final String rawJSON) {
    return this.payload(rawJSON, GCloudCommands.class);
  }

  private final <T> Optional<T> payload(final String rawJSON, final Class<T> clazz) {
    try {
      final T cmd = this.gson.fromJson(rawJSON, clazz);
      return Optional.<T>fromNullable(cmd);
    } catch(Exception ex) {
      ex.printStackTrace(System.err);
      logger.error("invalid 'gcloud' payload: {}", rawJSON);
      logger.error("failed to parse json", ex);
    }
    return Optional.<T>absent();
  }

}
