FROM eclipse-temurin:17-jdk-alpine as java_build
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

RUN --mount=type=cache,target=/root/.m2 ./mvnw -Pgcloud clean install -DskipTests
RUN mkdir -p target/dependency

WORKDIR /workspace/app/target/dependency

RUN jar -xf /workspace/app/gcloud/target/gcloud.jar

FROM eclipse-temurin:17

ARG DEPENDENCY=/workspace/app/target/dependency

VOLUME /tmp

COPY --from=java_build ${DEPENDENCY} /app

COPY ./faults/faults-command-runner.properties /faults-command-runner.properties
COPY ./faults/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
