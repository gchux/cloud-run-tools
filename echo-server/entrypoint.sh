#!/usr/bin/env bash

if [[ -z "${ECHO_CONFIG}" ]]; then
  export ECHO_CONFIG='/x/echo-server.properties'
fi

set -x

exec java \
  -Decho.log.level=${ECHO_LOG_LEVEL:-INFO} \
  -cp /x/app dev.chux.gcp.crun.echo.EchoServerApp \
  --config=${ECHO_CONFIG}
