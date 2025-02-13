#!/usr/bin/env bash

if [[ -z "${FAULTS_CONFIG}" ]]; then
  export FAULTS_CONFIG='/x/echo-server.properties'
fi

set -x

exec java -cp /x/rest_app \
  dev.chux.gcp.crun.echo.EchoServerApp \
  --config=${FAULTS_CONFIG}
