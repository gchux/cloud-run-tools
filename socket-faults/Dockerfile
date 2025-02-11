FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY api api
COPY core core
COPY exec exec
COPY rest rest
COPY http http

COPY socket-faults socket-faults

RUN --mount=type=cache,target=/root/.m2 ./mvnw -Psocket_faults clean install -DskipTests
RUN mkdir -p target/dependency

WORKDIR /workspace/app/target/dependency

RUN jar -xf /workspace/app/socket-faults/target/socket-faults.jar

FROM eclipse-temurin:17

ARG DEPENDENCY=/workspace/app/target/dependency

VOLUME /tmp

COPY --from=build ${DEPENDENCY} /app

COPY ./socket-faults/socket-faults-generator.properties /socket-faults-generator.properties
COPY ./socket-faults/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
