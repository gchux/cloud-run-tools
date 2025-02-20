package dev.chux.gcp.crun.gcloud.rest;

import java.io.OutputStream;
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
import static com.google.common.base.Throwables.getStackTraceAsString;

import static java.nio.charset.StandardCharsets.UTF_8;

import static java.util.Collections.singletonList;

public class RunGCloudCommandController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(RunGCloudCommandController.class);

  private static final String OUT_STDOUT = "stdout";

  public static final String KEY = RestModule.NAMESPACE + "/exec";

  private final Gson gson;
  private final GCloudService gcloudService;

  @Inject
  public RunGCloudCommandController(final Gson gson, final GCloudService gcloudService) {
    this.gson = gson;
    this.gcloudService = gcloudService;
  }

  /**
   * # Single command execution:
   *   - Description: executes 1 Google Cloud CLI command
   *   - Endpoints:
   *     - `POST /gcloud/exec`
   *     - `POST /gcloud/exec/`
   *     - `POST /gcloud/exec[/:namespace]`
   *     - `POST /gcloud/exec[/:namespace/]`
   *   - Path Params:
   *     - namespace: Optional<String> = OneOf<linux | java | python | nodejs | golang>
   *   - JSON Payload:
   *     - type: `GCloudCommand`
   *     - source: <root>/model/src/main/java/dev/chux/gcp/crun/model/GCloudCommand.java
   *     - JSON payload:
   *       {
   *         "namespace": Optional<String>                // main group, (optional if path param `namespace` is defined); i/e: `run`, `app`, `storage`, etc...
   *         "groups": Optional<List<String>>             // seconsary groups; i/e for namespace `run`: [`services`] or [`revisions`], etc...
   *         "command": Required<String>                  // action; i/e for namespace `run` and groups [`services`]: `update`, `delete`, etc...
   *         "flags" Optional<Map<String, String>>        // map of flag names –without dashes– to values; i/e: `{"project":"sample-project"}`
   *         "arguments": Optional<List<String>>          // i/e for namespace `run`, groups [`services`], and action `delete`: `service-name` 
   *         "format": Optional<String>                   // https://cloud.google.com/sdk/gcloud/reference#--format ; i/e: `text`, `json`, `yaml`, etc...
   *         "project": Optional<String>                  // https://cloud.google.com/sdk/gcloud/reference#--project ; i/e: `test-project`
   *         "environment": Optional<Map<String, String>> // map of environment variables names to values; i/e: `{"CLOUDSDK_CORE_DISABLE_PROMPTS":"1"}`
   *         "verbosity": Optional<String>                // https://cloud.google.com/sdk/gcloud/reference#--verbosity ; i/e: `debug`
   *         "log/http": Optional<Boolean>                // https://cloud.google.com/sdk/gcloud/reference#--log-http ; OneOf<true | false>
   *       }
   *   - Samples:
   *     - `POST /gcloud/exec/run`
   *       ```json
   *       {
   *         "namespace": "run",
   *         "groups": ["services"],
   *         "command": "delete",
   *         "flags": {
   *           "verbosity": "debug"
   *         },
   *         "arguments": ["sample-service"],
   *         "format": "text",
   *         "project": "test-project",
   *         "environment": {
   *           "CLOUDSDK_CORE_DISABLE_PROMPTS": "1"
   *         }
   *       }
   *       ```
   *
   * ---
   *
   * # Multiple command execution:
   *   - Description: executes 1 or more Google Cloud CLI command
   *   - Endpoints:
   *     - POST /gcloud/exec
   *     - POST /gcloud/exec/
   *   - Query Params:
   *     - type: Required<String> = OneOf<batch | multi>
   *   - JSON payload:
   *     - type: `Multivalue<GCloudCommand>`
   *     - source: <root>/model/src/main/java/dev/chux/gcp/crun/model/GCloudCommands.java
   *     {
   *       "(items|values)": List<GCloudCommand>   // list of simple Google Cloud CLI commands to be executed
   *     }
   *   - Samples:
   *     - `POST /gcloud/exec?type=batch`
   *       ```json
   *       {
   *         "items": [
   *           {
   *             "namespace": "run",
   *             "groups": ["services"],
   *             "command": "delete",
   *             "flags": {
   *               "verbosity": "debug"
   *             }
   *             "arguments": ["sample-service"],
   *             "format": "json"
   *             "project": "test-project",
   *             "environment": {
   *               "CLOUDSDK_CORE_DISABLE_PROMPTS": "1"
   *             }
   *           },
   *           {
   *             "namespace": "app",
   *             "groups": ["services"],
   *             "command": "delete",
   *             "flags": {
   *               "verbosity": "debug"
   *             },
   *             "arguments": ["sample-service"],
   *             "format": "text"
   *             "project": "test-project",
   *             "environment": {
   *               "CLOUDSDK_CORE_DISABLE_PROMPTS": "1"
   *             }
   *           }
   *         ]
   *       }
   *       ```
   */

  public void register(final String root) {
    path(root, () -> {
      post("/exec", "*/*", this);
      path("/exec", () -> {
        post("/", "*/*", this);
        post("/:namespace", "*/*", this);
        post("/:namespace/", "*/*", this);
      });
    });
  }

  public String endpoint(final String root) {
    return "POST " + root + "/exec[/[optional:namespace]][/?[type=(batch|multi)]]";
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    final String executionID = UUID.randomUUID().toString();

    final String rawJSON = request.body();
    final String namespace = emptyToNull(request.params(":namespace"));
    final boolean isMultiple = isMultiple(request, "type");

    response.header("x-gcloud-execution-id", executionID);

    logger.info("starting: {}", executionID);

    logger.debug("namespace: {}", namespace);
    logger.debug("rawJSON: {}", rawJSON);
    logger.debug("isMultiple: {}", isMultiple);

    final Optional<GCloudCommands> commands;
    if (isMultiple) {
      commands = this.commandsPayload(rawJSON);
    } else {
      commands = toGCloudCommands(
        fromNullable(namespace), this.commandPayload(rawJSON)
      );
    }

    logger.debug("commands: {}", commands);

    if ( commands.isPresent() ) {
      this.runCommands(request, response, commands.get());
    } else {
      logger.error("invalid 'gcloud' payload: {}", rawJSON);
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

  private final boolean runCommand(
    final Request request,
    final Response response,
    final OutputStream stream,
    final GCloudCommand command
  )  throws Exception {
    checkNotNull(command);
    
    if (isNullOrEmpty(command.namespace())) {
      logger.error("GCLOUD command without namespace: {}", command);
      return false;
    }

    logger.info("running GCLOUD command: {}", command);

    final String output = request.queryParams("output"); 

    if( isNullOrEmpty(output) ) {
      this.gcloudService.run(command, stream);
    } else {
      this.gcloudService.run(command);
    }
    
    return true;
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

    final OutputStream stream = response.raw().getOutputStream();
    final OutputStreamWriter writer = new OutputStreamWriter(stream, UTF_8);

    int index = 0;
    for (final GCloudCommand command : commands.get()) {
      writer.append("\n>>> COMMAND[")
        .append(Integer.toString(index+1, 10))
        .append("] >>>\n\n")
        .flush();

      final boolean ok = this.runCommand(request, response, stream, command);

      writer.append("\n<<< COMMAND[")
        .append(Integer.toString(++index, 10))
        .append("] = ")
        .append(ok ? "SUCCESS" : "FAILED")
        .append(" <<<\n\n")
        .flush();
    }
  }

  private final Optional<GCloudCommand> commandPayload(final String rawJSON) {
    try {
      return jsonPayload(this.gson, rawJSON, GCloudCommand.class);
    } catch(final Exception ex) {
      logger.error("failed to parse json", getStackTraceAsString(ex));
    }
    return absent();
  }

  private final Optional<GCloudCommands> commandsPayload(final String rawJSON) {
    try {
      return jsonPayload(this.gson, rawJSON, GCloudCommands.class);
    } catch(final Exception ex) {
      logger.error("failed to parse json", getStackTraceAsString(ex));
    }
    return absent();
  }

}
