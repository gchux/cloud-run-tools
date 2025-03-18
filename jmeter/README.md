# JMeter as a Service

## Motivation

During development it is often useful to perform HTTP load tests on microservices to confirm different behaviors under stress.

[Apache JMeter](https://jmeter.apache.org/) is a well known open source tool to perform flexible load testing; however,
it requires to create test cases either using JMX or the JMeter UI which requires some knowledge and consumes valuable time.

The goal of this project is to provide common load testing – parametrizable – scenarios, bundle them into a container running JMeter,
and enable test execution using a simple REST API that can be invoked using simple tools as [cURL](https://curl.se/).

Additionally, this project aims to provide compatibility with [Cloud Run](https://cloud.google.com/run) for both:

- hosting JMeter as a Cloud Run service.
- load test Cloud Run services that require authentication.

## How to use

### Query Parameters

#### Basic Parameters

- **`test`**: [`String`, _optional_, default:`test`] [test scenario](src/main/jmeter) to use; without `.jmx` extension.
- **`mode`**: [`String`, _optional_, default:`concurrency`] test operation mode; alternatives: `qps` or `concurrency`.
- **`proto`**: [`String`, _optional_, default:`https`] protocol to use; alternatives: `http` or `https`.
- **`method`**: [`String`, _optional_, default:`GET`] [HTTP method](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods) to use.
- **`host`**: [`String`, **required**]: hostname or IP of the remote HTTP server.
- **`port`**: [`Integer`, _optional_, default:`443`] TCP port used to connect to the remote HTTP server.
- **`path`**: [`String`, _optional_, default:`/`] endpoint to test on the remote HTTP server.
- **`duration`**: [`Integer`, **required**] test duration in seconds.

#### Latency Parameters

- **`min_latency`**: [`Integer`, _optional_, default:`1`] remote service minimum response time in milliseconds.
- **`max_latency`**: [`Integer`, _optional_, default:`1000`] remote service maxium response time in milliseconds.

#### `concurrency` mode Parameters

- **`steps`**: [`List<`**`Tuple<Integer>`**`>`, **required**] list of steps in the form of `5-tuples` containing each step configuration.

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

    ```
    GET /jmeter/test/run?mode=concurrency&duration=11&test=generic_dynamic&steps=10,0,0,10,1 HTTP/1.1
    ```

  - `10,0,0,10,1` `;` `10,5,10,10,1`:

    - `step[1]`: immediately start 10 threads, hold the load for 10 seconds, and stop in 1 second; step duration is 11 seconds.
    - `step[2]`: start 10 threads over 10 seconds after 5 seconds of startig the test, hold the load for 10 seconsa, and stop within 1 second; step duration is 21 seconds.

    ```
    GET /jmeter/test/run?mode=concurrency&duration=32&test=generic_dynamic&steps=10,0,0,10,1;10,5,10,10,1 HTTP/1.1
    ```

> [!IMPORTANT]
> Query param **`duration`** must be equal to the sum of _`rampup_time + duration + shutdown_time`_ across all `5-tuples`.

> [!NOTE]
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

    ```
    GET /jmeter/test/run?mode=qps&duration=80&test=generic_qps&qps=1,10,10;10,10,60;10,0,10 HTTP/1.1
    ```

> [!IMPORTANT]
> Query param **`duration`** must be equal to the sum of _`duration`_ across all `3-truples`.

> [!NOTE]
> See: https://jmeter-plugins.org/wiki/ThroughputShapingTimer/

#### `test` Parameter

Depending on the value of the **`mode`** parameter, **`test`** may be one of:

- If **`mode`** is set to **`qps`**:

  - `cloud_run_qps`
  - `generic_qps`

- If **`mode`** is set to **`concurrency`**:

  - `cloud_run_dynamic`
  - `generic_dynamic`

## Pre-Built images

- ghcr.io/gchux/cloud-run-tools:jmaas-latest

## Samples

```shell
curl -ivL \
  '0:8080/jmeter/test/run?mode=qps&test=cloud_run_qps&host=hello-0000000000.us-central1.run.app&path=/&duration=375&qps=1,100,5;100,100,120;100,50,5;50,50,120;50,10,5;10,10,120'
```

![jmaas_test_ui](https://github.com/gchux/jmeter-test-runner/blob/main/jmeter/img/jmaas_test_ui.png?raw=true)

![jmaas_test_shell](https://github.com/gchux/jmeter-test-runner/blob/main/jmeter/img/jmaas_test_shell.png?raw=true)
