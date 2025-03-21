#!/usr/bin/env bash

source /.cloud_run.rc

if [[ -z "${GCLOUD_CONFIG}" ]]; then
  export GCLOUD_CONFIG='/gcloud-command-runner.properties'
fi

set -x

exec java \
  -Dgcloud.log.level=${GCLOUD_LOG_LEVEL:-INFO} \
  -Dcom.google.cloud.project.id=${GCP_PROJECT_ID} \
  -Dcom.google.cloud.project.num=${GCP_PROJECT_NUM} \
  -Dcom.google.cloud.run.region=${GCP_REGION} \
  -Dcom.google.cloud.run.service=${K_SERVICE} \
  -Dcom.google.cloud.run.revision=${K_REVISION} \
  -Dcom.google.cloud.run.instance.id=${CLOUD_RUN_INSTANCE_ID} \
  dev.chux.gcp.crun.gcloud.GCloudApp \
  --config=${GCLOUD_CONFIG}
