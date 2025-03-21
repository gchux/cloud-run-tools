FROM eclipse-temurin:17-jdk-alpine-3.20 AS build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY api api
COPY annotations annotations
COPY model model
COPY core core
COPY exec exec
COPY rest rest
COPY http http

COPY echo-server echo-server

RUN --mount=type=cache,target=/root/.m2 ./mvnw -Pecho_server wrapper:wrapper clean install -DskipTests -Dmaven=3.9.9
RUN mkdir -p target/dependency

WORKDIR /workspace/app/target/dependency

RUN jar -xf /workspace/app/echo-server/target/echo-server.jar

FROM eclipse-temurin:17-alpine-3.20

ARG DEPENDENCY=/workspace/app/target/dependency

VOLUME /tmp

RUN apk add --update --no-cache bash curl

COPY --from=build ${DEPENDENCY} /x/app

COPY ./cloud_run.rc /.cloud_run.rc
COPY ./echo-server/echo-server.properties /x/echo-server.properties
COPY ./echo-server/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
