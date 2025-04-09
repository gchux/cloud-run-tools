# JMaaS ( JMeter as a Service )

![Static Badge](https://img.shields.io/badge/v1.6.7-green?style=flat&label=latest%20version&labelColor=gray&color=green&link=https%3A%2F%2Fgithub.com%2Fgchux%2Fcloud-run-tools%2Fpkgs%2Fcontainer%2Fcloud-run-tools%2F383324176%3Ftag%3Djmaas-v1.5.2)

## Motivation

During development it is often useful to perform **`HTTP/1.1`** load tests on microservices to confirm different behaviors under stress.

[Apache JMeter](https://jmeter.apache.org/) is a well known open source tool to perform flexible load testing; however,
it entails creating complex test cases either using JMX or the JMeter UI which requires some knowledge and consumes valuable time.

The goal of this project is to provide common load testing – parametrizable – scenarios, and enable test execution via REST API endpoints  
that can be invoked using simple tools as [cURL](https://curl.se/); all of it bundled into a worker container ready to be deployed with minimal friction.

Additionally, this project aims to provide compatibility with [Cloud Run](https://cloud.google.com/run) for:

- hosting JMeter as a Cloud Run service ( also known as JMaaS ).
- load test Cloud Run services that require authentication.
- Propagate [cloud trace context](https://cloud.google.com/trace/docs/trace-context) to show load test traffic in [Cloud Trace Explorer](https://cloud.google.com/trace/docs/finding-traces).

## Get test details

- using path parameters:

  ```http
  GET /jmeter/test/status/:id
  Accept: application/json
  ```

  ```http
  HEAD /jmeter/test/status/:id
  ```

- using query string parameters:

  ```http
  GET /jmeter/test/status?id=<test-id>
  Accept: application/json
  ```

  ```http
  HEAD /jmeter/test/status?id=<test-id>
  ```

- using header parameters:

  ```http
  GET /jmeter/test/status
  Accept: application/json
  x-jmaas-test-id: <test-id>
  ```

  ```http
  HEAD /jmeter/test/status
  x-jmaas-test-id: <test-id>
  ```

### Path Parameters

- **`id`**: [`String`, **required**] - test ID to be queried.

### Query Parameters

- **`id`**: [`String`, **required**] - test ID to be queried.

### Headers

- **`Accept`**: [`String`, **required**] - must be `application/json`.
- **`x-jmaas-test-id`**: [`String`, **required**] - test ID to be queried.

> [!NOTE]
> Pass the **required** test **`id`** however you prefer, either using path, query, or header parameters.

## Stream test output

```http
GET /jmeter/test/stream[/:id][?id=<test-id>]
Accept: text/plain
Content-Length: 0
[x-jmaas-test-id: <test-id>]
```

### Path Parameters

- **`id`**: [`String`, **required**] - ID of the test's output to be streamed.

### Query Parameters

- **`id`**: [`String`, **required**] - test ID to be streamed.

### Headers

- **`Accept`**: [`String`, **required**] - must be `text/plain`.
- **`x-jmaas-test-id`**: [`String`, **required**] - test ID to be streamed.

> [!NOTE]
> Only 1 request is allowed to stream the output of a test execution.

## Run tests

### Endpoints

- without payload:
  - `GET /jmeter/test/run`
  - `GET /jmeter/test/run/:id`
- with payload:
  - `POST /jmeter/test/run`
  - `POST /jmeter/test/run/:id`

### Request Payload

Request payload is automatically propagated from the original request.

### Test Parameters

Test parameters are passed as URL query parameters by default; however, it is also possible to pass them as request headers prefixed by `x-jmaas-test-*`.

> [!TIP]
> You may combine query and headers test params in the same request.
>
> Query test parameters are checked first, then headers will be used as a fallback.

### Query Parameters

#### Basic Parameters

- **`async`**: [`Boolean`, _optional_, default:`false`] wether to wait for test execution to be complete or to return immediately.
- **`id`**: [`String`, _optional_] test ID passed to the remote service via HTTP request header `x-jmaas-test-id`; if not present, a random `UUID` will be used.
- **`script`**: [`String`, _optional_, default:`generic_dynamic_full`] [script options](#script-parameter) to use; without `.jmx` extension.
- **`mode`**: [`String`, _optional_, default:`concurrency`] test operation mode; alternatives: [`qps`](#qps-mode-parameters) or [`concurrency`](#concurrency-mode-parameters).
- **`proto`**: [`String`, _optional_, default:`https`] protocol to use; alternatives: `http` or `https`.
- **`method`**: [`String`, _optional_, default:`GET`] [HTTP method](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods) to use.
- **`host`**: [`String`, **required**]: hostname or IP of the remote HTTP server.
- **`port`**: [`Integer`, _optional_, default:`443`] TCP port used to connect to the remote HTTP server.
- **`path`**: [`String`, _optional_, default:`/`] endpoint to test on the remote HTTP server.
- **`duration`**: [`Integer`, **required**] test duration in seconds.
- **`params`**: [`Map<String, String>`, _optional_] query parameters to send; sample: `params=paramA:A;paramB:B`.
- **`headers`**: [`Map<String, string>`, _optional_] headers to send; sample: `headers=headerA:A;headerB:B`.
- **`output`**: [`String`,_optional_, default:`res`] where to stream the output of the test; alternatives: `res` for response, and `sys` for standard output.

> [!NOTE]
> When passing test parameters as request headers, replace all underscores (`_`) by dashes (`-`).

> [!TIP]
> When using headers to pass **`params`** or **`headers`**, you may use an equals sign (`=`) to separate name from value.
>
> > For example: `x-jmaas-test-headers: Content-Type=text/plain;x-header-name=header_value`

#### Latency Parameters

- **`min_latency`**: [`Integer`, _optional_, default:`1`] remote service minimum response time in milliseconds.
- **`max_latency`**: [`Integer`, _optional_, default:`1000`] remote service maximum response time in milliseconds.

#### `concurrency` mode Parameters

- **`concurrency`**: [`List<`**`Tuple<Integer>`**`>`, **required**] list of steps in the form of `5-tuples` containing each step configuration.

  `5-tuples` are separated by `;`, and tuple items are separated by `,`.

  Each `5-tuple` must contain the following information:

  - `[1]`: **`thread_count`** - how many threads to use for this step.
  - `[2]`: **`initial_delay`** - number of seconds after which this step should start.
  - `[3]`: **`rampup_time`** – number of seconds required to reach **`thread_count`**.
  - `[4]`: **`duration`** – number of seconds this step should apply **`thread_count`** after **`rampup_time`**.
  - `[5]`: **`shutdown_time`** - number of seconds required to drop **`thread_count`** to `0`.

  `5-tuple` examples:

  - `10,0,0,10,1`:

    - `step[1]`: immediately start 10 threads, hold the load for 10 seconsa, and stop in 1 second.

    ```http
    POST /jmeter/test/run?concurrency=10,0,0,10,1 HTTP/1.1
    Accept: text/plain
    x-jmaas-test-id: load-test-0001
    x-jmaas-test-mode: concurrency
    x-jmaas-test-duration: 11
    x-jmaas-test-script: generic_dynamic
    x-jmaas-test-proto: http
    x-jmaas-test-method: POST
    x-jmaas-test-host: localhost
    x-jmaas-test-port: 8080
    x-jmaas-test-path: /
    x-jmaas-test-headers: Content-type=text/plain

    test
    ```

> [!IMPORTANT]
> When running tests with payload, it is not required and stronly discouraged to pass `Content-Length` via `x-jmaas-test-headers` as this will be automatically done by **`JMaaS`** during test instrumentation.

- `10,0,0,10,1` `;` `10,5,10,10,1`:

  - `step[1]`: immediately start 10 threads, hold the load for 10 seconds, and stop in 1 second; step duration is 11 seconds.
  - `step[2]`: start 10 threads over 10 seconds after 5 seconds of startig the test, hold the load for 10 seconsa, and stop within 1 second; step duration is 21 seconds.

  ```http
  GET /jmeter/test/run/load-test-0002 HTTP/1.1
  Accept: text/plain
  Content-Length: 0
  x-jmaas-test-mode: concurrency
  x-jmaas-test-duration: 32
  x-jmaas-test-script: generic_dynamic
  x-jmaas-test-concurrency: 10,0,0,10,1;10,5,10,10,1
  ```

> [!IMPORTANT]
> Query param **`duration`** must be equal to the sum of _`rampup_time + duration + shutdown_time`_ across all `5-tuples`.

> [!TIP]
> See: https://jmeter-plugins.org/wiki/UltimateThreadGroup/

#### `qps` mode Parameters

- **`qps`**: [`List<`**`Tuple<Integer>`**`>`, **required**] list of steps in the form of `3-tuples` containing each step configuration.

  `3-tuples` are separated by `;`, and tuple items are separated by `,`.

  Each `3-tuple` must contain the following information:

  - `[1]`: **`start_qps`** - initial number of QPS.
  - `[2]`: **`end_qps`** - final number of QPS.
  - `[3]`: **`duration`** – number of seconds over which **`end_qps`** will be reached, starting from **`start_qps`**.

  `3-tuple` examples:

  - `1,10,10` `;` `10,10,60` `;` `10,0,10`:

    - `step[1]`: ramp up from 1 to 10 QPS over 10 seconds.
    - `step[2]`: hold 10 QPS for 60 seconds.
    - `step[3]`: drop QPS from 10 to 0 in 10 seconds.

    ```http
    GET /jmeter/test/run HTTP/1.1
    Accept: text/plain
    Content-Length: 0
    x-jmaas-test-id: load-test-0003
    x-jmaas-test-mode: qps
    x-jmaas-test-duration: 80
    x-jmaas-test-script: generic_qps
    x-jmaas-test-qps: 1,10,10;10,10,60;10,0,10
    ```

> [!IMPORTANT]
> Query param **`duration`** must be equal to the sum of _`duration`_ across all `3-truples`.

> [!TIP]
> See: https://jmeter-plugins.org/wiki/ThroughputShapingTimer/

#### `script` Parameter

Depending on the value of the **`mode`** parameter, the **`script`** parameter may be one of:

- If **`mode`** is set to **`qps`**:

  - `cloud_run_qps`
  - `cloud_run_qps_full`
  - `generic_qps`
  - `generic_qps_full`

- If **`mode`** is set to **`concurrency`**:

  - `cloud_run_dynamic`
  - `cloud_run_dynamic_full`
  - `generic_dynamic`
  - `generic_dynamic_full`

> [!IMPORTANT]
> Only `*_full` tests support: query parameters, headers and body.

## Pre-Built images

- ghcr.io/gchux/cloud-run-tools:jmaas-latest

## Hosting in Cloud Run

1. Pull the pre-built image and upload it to Artifact Registry:

```bash
export GCP_PROJECT_ID='...'
export ARTIFACT_REGISTRY_REGION='...'
export ARTIFACT_REGISTRY_REPOSITORY='...'
export ARTIFACT_REGISTRY_IMAGE_URI="${ARTIFACT_REGISTRY_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${ARTIFACT_REGISTRY_REPOSITORY}/cloud-run-tools:jmaas-latest"

docker pull ghcr.io/gchux/cloud-run-tools:jmaas-latest
docker tag ghcr.io/gchux/cloud-run-tools:jmaas-latest ${IMAGE_URI_FULL}
docker push ${ARTIFACT_REGISTRY_IMAGE_URI}
```

2. Deploy the container into Cloud Run:

```bash
export CLOUD_RUN_SERVICE_NAME='...'
export CLOUD_RUN_SERVICE_REGION='...'
export CLOUD_RUN_SERVICE_ACCOUNT='...@<project-id>.iam.gserviceaccount.com'

gcloud run deploy ${CLOUD_RUN_SERVICE_NAME} \
  --image=${ARTIFACT_REGISTRY_IMAGE_URI} \
  --region=${CLOUD_RUN_SERVICE_REGION} \
  --service-account=${CLOUD_RUN_SERVICE_ACCOUNT} \
  --execution-environment=gen2 \
  --no-use-http2 \
  --no-cpu-throttling \
  --session-affinity \
  --no-allow-unauthenticated \
  --concurrency=2 \
  --min-instances=0 \
  --max-instances=10 \
  --cpu=2 \
  --memory=2Gi \
  --timeout=3600s \
  --port=8080
```

> [!IMPORTANT]
> Make sure that the [Service Identity](https://cloud.google.com/run/docs/securing/service-identity) given by `CLOUD_RUN_SERVICE_ACCOUNT` has enough permissions to invoke the remote service.

> [!TIP]
> It is recommended to set [maximum concurrency](https://cloud.google.com/run/docs/about-concurrency) to 2, and allocate enough CPU and Memory according to the amount of traffic required to be prodiced by a single instance. The 2nd request slot is only used for [get test detail](#get-test-details) or [stream test output](#stream-test-output) requests.

## Samples

- `mode=qps` `&` `duration=1800` `&` `qps=` `1,50,60` `;` `50,100,60` `;` `100,100,300` `;` `100,50,60` `;` `50,50,300` `;` `50,100,60` `;` `100,100,300` `;` `100,50,60` `;` `50,50,300` `;` `50,0,300`

  > ![jmaas_test_ui](https://github.com/gchux/jmeter-test-runner/blob/main/jmeter/img/jmaas_test_ui.png?raw=true)
