FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN --mount=type=cache,target=/root/.m2 ./mvnw install -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../app.jar)

FROM eclipse-temurin:17
ARG JMETER_VERSION="5.6.2"
ENV JMETER_HOME /opt/apache-jmeter-${JMETER_VERSION}
ENV	JMETER_BIN ${JMETER_HOME}/bin
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY} /app

RUN curl -L https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz > /tmp/jmeter.tgz \
    && mkdir -p /opt \
    && tar -xvf /tmp/jmeter.tgz -C /opt \
    && rm /tmp/jmeter.tgz \
    && ln -s /opt/apache-jmeter-${JMETER_VERSION} /opt/jmeter \
    && ln -s /opt/jmeter/bin/jmeter /usr/bin/jmeter \
    && cd /opt/apache-jmeter-${JMETER_VERSION}/lib \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/cmdrunner/2.3/cmdrunner-2.3.jar \
    && cd /opt/apache-jmeter-${JMETER_VERSION}/lib/ext/ \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-manager/1.9/jmeter-plugins-manager-1.9.jar \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-casutg/2.10/jmeter-plugins-casutg-2.10.jar \
    && curl -L -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-fifo/0.2/jmeter-plugins-fifo-0.2.jar \
    && curl -L -O https://github.com/QAInsights/validate-thread-group/releases/download/v1.0.1/validatetg-1.0.1.jar \
    && java -cp jmeter-plugins-manager-1.9.jar org.jmeterplugins.repository.PluginManagerCMDInstaller \
    && cd /opt/apache-jmeter-${JMETER_VERSION}/bin && ./PluginsManagerCMD.sh install jpgc-casutg,jpgc-fifo,jpgc-json

COPY print_id_token /print_id_token
COPY test.jmx /test.jmx

ENV PATH $PATH:$JMETER_BIN

ENTRYPOINT ["java","-cp","/app","dev.chux.gcp.crun.App"]
