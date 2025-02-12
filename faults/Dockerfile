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

WORKDIR /curl/java

COPY faults/src/curl/java/.mvn .mvn
COPY faults/src/curl/java/mvnw mvnw
COPY faults/src/curl/java/pom.xml pom.xml
COPY faults/src/curl/java/src src

RUN --mount=type=cache,target=/root/.m2 ./mvnw wrapper:wrapper clean install -DskipTests -Dmaven=3.9.9

FROM node:lts-alpine3.20 AS curl_nodejs_build

WORKDIR /curl/nodejs

COPY faults/src/curl/nodejs/curl.js .
COPY faults/src/curl/nodejs/package.json .
COPY faults/src/curl/nodejs/package-lock.json .

RUN npm install

FROM python:3.12.9-alpine3.20 AS curl_python_build

WORKDIR /curl/python

COPY faults/src/curl/python/curl.py .
COPY faults/src/curl/python/requirements.txt .

RUN python3 -m venv /curl/python
ENV PATH="/curl/python/bin:${PATH}"
RUN pip install --no-cache-dir -U -r requirements.txt
RUN rm -vf /curl/python/bin/python*

FROM golang:1.23-alpine3.20 AS curl_golang_build

WORKDIR /curl/golang

COPY faults/src/curl/golang/curl.go .
COPY faults/src/curl/golang/go.mod .
COPY faults/src/curl/golang/go.sum .

RUN go mod tidy
RUN go build -a -v -o curl curl.go

FROM eclipse-temurin:17-alpine-3.20

ARG DEPENDENCY=/workspace/app/target/dependency

VOLUME /tmp

COPY --from=build ${DEPENDENCY} /app

RUN apk add --update --no-cache bash nodejs python3=3.12.9-r0

COPY --from=curl_java_build /curl/java/target/curl.jar /curl/java/curl.jar
COPY faults/src/curl/java/curl-java /curl_java

COPY --from=curl_nodejs_build /curl/nodejs /curl/nodejs
COPY faults/src/curl/nodejs/curl-nodejs /curl_nodejs

COPY --from=curl_python_build /curl/python /curl/python
COPY faults/src/curl/python/curl-python /curl_python
RUN ln -s "$(which python3.12)" /curl/python/bin/python \
  && ln -s "$(which python3.12)" /curl/python/bin/python3 \
  && ln -s "$(which python3.12)" /curl/python/bin/python3.12

COPY --from=curl_golang_build /curl/golang /curl/golang
COPY faults/src/curl/golang/curl-golang /curl_golang

COPY ./faults/faults-generator.properties /faults-generator.properties
COPY ./faults/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
