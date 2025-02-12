#!/usr/bin/env bash

if [[ -z "${FAULTS_CONFIG}" ]]; then
  export FAULTS_CONFIG='/x/faults-generator.properties'
fi

set -x

exec java -cp /x/rest_app \
  dev.chux.gcp.crun.faults.FaultsApp \
  --config=${FAULTS_CONFIG}
