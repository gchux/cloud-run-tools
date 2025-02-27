#!/usr/bin/env bash

if [[ -z "${JMETER_CONFIG}" ]]; then
  export JMETER_CONFIG='/jmeter-test-runner.properties'
fi

set -x

exec java \
  -Djmeter.log.level=${JMETER_LOG_LEVEL:-INFO} \
  -cp /app dev.chux.gcp.crun.jmeter.JMeterApp \
  --config=${JMETER_CONFIG}
