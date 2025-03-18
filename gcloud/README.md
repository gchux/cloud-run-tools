# Google Cloud SDK as a service

## Single command execution:

- **Description**: executes 1 Google Cloud CLI command

- **Endpoints**:

  - `POST /gcloud/exec`
  - `POST /gcloud/exec/`
  - `POST /gcloud/exec[/:namespace]`
  - `POST /gcloud/exec[/:namespace/]`

- **Path Params**:

- **JSON Payload**:

  - **`type`**: `GCloudCommand`
  - **`source`**: [`GCloudCommands.java`](https://github.com/gchux/cloud-run-tools/blob/main/model/src/main/java/dev/chux/gcp/crun/model/GCloudCommands.java)

    ```json
    {
      "namespace":   Optional<String>              // main group, (optional if path param `namespace` is defined); i/e: `run`, `app`, `storage`, etc...
      "groups":      Optional<List<String>>        // seconsary groups; i/e for namespace `run`: [`services`] or [`revisions`], etc...
      "command":     Required<String>              // action; i/e for namespace `run` and groups [`services`]: `update`, `delete`, etc...
      "flags"        Optional<Map<String, String>> // map of flag names –without dashes– to values; i/e: `{"project":"sample-project"}`
      "arguments":   Optional<List<String>>        // i/e for namespace `run`, groups [`services`], and action `delete`: `service-name`
      "format":      Optional<String>              // https://cloud.google.com/sdk/gcloud/reference#--format ; i/e: `text`, `json`, `yaml`, etc...
      "project":     Optional<String>              // https://cloud.google.com/sdk/gcloud/reference#--project ; i/e: `test-project`
      "environment": Optional<Map<String, String>> // map of environment variables names to values; i/e: `{"CLOUDSDK_CORE_DISABLE_PROMPTS":"1"}`
      "verbosity":   Optional<String>              // https://cloud.google.com/sdk/gcloud/reference#--verbosity ; i/e: `debug`
      "log/http":    Optional<Boolean>             // https://cloud.google.com/sdk/gcloud/reference#--log-http ; OneOf<true | false>
    }
    ```

### Samples:

- `POST /gcloud/exec/run`

  ```json
  {
    "namespace": "run",
    "groups": ["services"],
    "command": "delete",
    "flags": {
      "verbosity": "debug"
    },
    "arguments": ["sample-service"],
    "format": "text",
    "project": "test-project",
    "environment": {
      "CLOUDSDK_CORE_DISABLE_PROMPTS": "1"
    }
  }
  ```

## Multiple command execution:

- **Description**: executes 1 or more Google Cloud CLI command

- **Endpoints**:

  - `POST /gcloud/exec`
  - `POST /gcloud/exec/`

- **Query Params**:

  - **`type`**: `Required<String>` = `OneOf<batch | multi>`

- **JSON payload**:

  - **`type`**: `List<GCloudCommand>`
  - **`source`**: [`GCloudCommands.java`](https://github.com/gchux/cloud-run-tools/blob/main/model/src/main/java/dev/chux/gcp/crun/model/GCloudCommands.java)

    ```json
    {
      "(items|values)": List<GCloudCommand> // list of simple Google Cloud CLI commands to be executed
    }
    ```

### Samples:

- `POST /gcloud/exec?type=batch`
  ```json
  {
    "items": [
      {
        "namespace": "run",
        "groups": ["services"],
        "command": "delete",
        "flags": {
          "verbosity": "debug"
        }
        "arguments": ["sample-service"],
        "format": "json"
        "project": "test-project",
        "environment": {
          "CLOUDSDK_CORE_DISABLE_PROMPTS": "1"
        }
      },
      {
        "namespace": "app",
        "groups": ["services"],
        "command": "delete",
        "flags": {
          "verbosity": "debug"
        },
        "arguments": ["sample-service"],
        "format": "text"
        "project": "test-project",
        "environment": {
          "CLOUDSDK_CORE_DISABLE_PROMPTS": "1"
        }
      }
    ]
  }
  ```
