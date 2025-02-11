#!/usr/bin/env bash

if [[ -z "${FAULTS_CONFIG}" ]]; then
  export FAULTS_CONFIG='/faults-command-runner.properties'
fi

set -x

exec java -cp /app \
  dev.chux.gcp.crun.faults.socket.SocketFaultsApp \
  --config=${FAULTS_CONFIG}
