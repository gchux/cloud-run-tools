FROM eclipse-temurin:17-jdk-alpine-3.20 AS build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY api api
COPY core core
COPY exec exec
COPY rest rest
COPY http http

COPY faults faults

RUN --mount=type=cache,target=/root/.m2 ./mvnw -Pfaults wrapper:wrapper clean install -DskipTests -Dmaven=3.9.9
RUN mkdir -p target/dependency

WORKDIR /workspace/app/target/dependency

RUN jar -xf /workspace/app/faults/target/faults.jar

FROM eclipse-temurin:17-jdk-alpine-3.20 AS curl_java_build

WORKDIR /workspace/curl

COPY faults/src/curl/java/.mvn .mvn
COPY faults/src/curl/java/* .

RUN --mount=type=cache,target=/root/.m2 ./mvnw wrapper:wrapper clean install -DskipTests -Dmaven=3.9.9

FROM node:lts-alpine3.20 AS curl_nodejs_build

WORKDIR /workspace/curl

COPY faults/src/curl/nodejs/curl.js .
COPY faults/src/curl/nodejs/package.json .
COPY faults/src/curl/nodejs/package-lock.json .

RUN npm install

FROM python:3-alpine3.20 AS curl_python_build

WORKDIR /workspace/curl

COPY faults/src/curl/python/curl.py .
COPY faults/src/curl/python/requirements.txt .

ENV VIRTUAL_ENV=/workspace/python
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"
RUN pip install --no-cache-dir -U -r requirements.txt

FROM golang:1.23-alpine3.20 AS curl_golang_build

WORKDIR /workspace/curl

COPY faults/src/curl/golang/curl.go .
COPY faults/src/curl/golang/go.mod .
COPY faults/src/curl/golang/go.sum .

RUN go build -a -v -o curl curl.go

FROM eclipse-temurin:17-alpine-3.20

ARG DEPENDENCY=/workspace/app/target/dependency

VOLUME /tmp

COPY --from=build ${DEPENDENCY} /app

RUN apk add --update --no-cache python3 nodejs

COPY --from=curl_java_build /workspace/curl/target/curl.jar /curl/java/curl.jar
COPY faults/src/curl/java/curl-java /curl-java

COPY --from=curl_nodejs_build /workspace/curl /curl/nodejs
COPY faults/src/curl/nodejs/curl-nodejs /curl-nodejs

COPY --from=curl_python_build /workspace/curl /curl/python
COPY faults/src/curl/python/curl-python /curl-python

COPY --from=curl_golang_build /workspace/curl /curl/golang
COPY faults/src/curl/golang/curl-golang /curl-golang

COPY ./faults/faults-generator.properties /faults-generator.properties
COPY ./faults/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
