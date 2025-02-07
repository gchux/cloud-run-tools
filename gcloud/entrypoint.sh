#!/usr/bin/env bash

if [[ -z "${GCLOUD_CONFIG}" ]]; then
  export GCLOUD_CONFIG='/gcloud-command-runner.properties'
fi

set -x

exec java -cp /app \
  dev.chux.gcp.crun.gcloud.GCloudApp \
  --config=${GCLOUD_CONFIG}
