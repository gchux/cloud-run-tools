#!/usr/bin/env bash

source /.cloud_run.rc

if [[ -z "${JMETER_CONFIG}" ]]; then
  export JMETER_CONFIG='/jmeter-test-runner.properties'
fi

set -x

exec java \
  -Djmeter.log.level=${JMETER_LOG_LEVEL:-INFO} \
  -Dcom.google.cloud.project.id=${GCP_PROJECT_ID} \
  -Dcom.google.cloud.project.num=${GCP_PROJECT_NUM} \
  -Dcom.google.cloud.run.region=${GCP_REGION} \
  -Dcom.google.cloud.run.service=${K_SERVICE} \
  -Dcom.google.cloud.run.revision=${K_REVISION} \
  -Dcom.google.cloud.run.instance.id=${CLOUD_RUN_INSTANCE_ID} \
  -cp /app dev.chux.gcp.crun.jmeter.JMeterApp \
  --config=${JMETER_CONFIG}
