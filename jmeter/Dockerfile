FROM eclipse-temurin:17-jdk-alpine AS build

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

COPY jmeter jmeter

RUN --mount=type=cache,target=/root/.m2 ./mvnw -Pjmeter clean install -DskipTests
RUN mkdir -p target/dependency

WORKDIR /workspace/app/target/dependency

RUN jar -xf /workspace/app/jmeter/target/jmeter.jar

FROM eclipse-temurin:17

ARG DEPENDENCY=/workspace/app/target/dependency
ARG JMETER_VERSION="5.6.3"

ENV JMETER_HOME=/opt/apache-jmeter-${JMETER_VERSION}
ENV JMETER_BIN=${JMETER_HOME}/bin

VOLUME /tmp

COPY --from=build ${DEPENDENCY} /app

RUN curl -L https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz > /tmp/jmeter.tgz \
    && mkdir -p /opt \
    && tar -xvf /tmp/jmeter.tgz -C /opt \
    && rm /tmp/jmeter.tgz \
    && ln -s /opt/apache-jmeter-${JMETER_VERSION} /opt/jmeter \
    && ln -s /opt/jmeter/bin/jmeter /usr/bin/jmeter

WORKDIR /opt/apache-jmeter-${JMETER_VERSION}/lib

RUN curl -L -O https://repo1.maven.org/maven2/kg/apc/cmdrunner/2.3/cmdrunner-2.3.jar

WORKDIR /opt/apache-jmeter-${JMETER_VERSION}/lib/ext/

RUN curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-manager/1.9/jmeter-plugins-manager-1.9.jar \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-casutg/3.0/jmeter-plugins-casutg-3.0.jar \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-tst/2.6/jmeter-plugins-tst-2.6.jar \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-functions/2.2/jmeter-plugins-functions-2.2.jar \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-httpraw/0.1/jmeter-plugins-httpraw-0.1.jar \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-fifo/0.2/jmeter-plugins-fifo-0.2.jar \
    && curl -L -O https://github.com/QAInsights/validate-thread-group/releases/download/v1.0.1/validatetg-1.0.1.jar \
    && java -cp jmeter-plugins-manager-1.9.jar org.jmeterplugins.repository.PluginManagerCMDInstaller

WORKDIR /opt/apache-jmeter-${JMETER_VERSION}/bin

RUN ./PluginsManagerCMD.sh install jpgc-casutg,jpgc-tst,jpgc-fifo,jpgc-functions,jpgc-json,jpgc-httpraw

WORKDIR /

RUN mkdir -p /jmx/

COPY ./cloud_run.rc /.cloud_run.rc
COPY ./jmeter/jmeter-test-runner.properties /jmeter-test-runner.properties
COPY ./jmeter/src/main/jmeter/print_id_token /print_id_token
COPY ./jmeter/entrypoint.sh /entrypoint.sh
COPY ./jmeter/src/main/jmeter/*.jmx /jmx/

ENV PATH=$PATH:$JMETER_BIN

ENTRYPOINT ["/entrypoint.sh"]
