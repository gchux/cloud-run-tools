FROM node:23-alpine3.20  AS ui

RUN apk add perl

COPY ./jmeter/ui /app

WORKDIR /app

RUN npm install
RUN npm install --global gulp-cli
RUN npm run build

RUN perl -i -p -e 's;(src|href)="/assets/;$1="/ui/assets/;g' ./dist/index.html

FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY api api
COPY io io
COPY annotations annotations
COPY model model
COPY core core
COPY exec exec
COPY rest rest
COPY http http

COPY jmeter jmeter

RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -Pjmeter clean install -DskipTests -Dmaven.test.skip=true
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -f jmeter/httpraw/pom.xml clean install -DskipTests -Dmaven.test.skip=true

RUN mkdir -p target/dependency

WORKDIR /workspace/app/target/dependency

RUN jar -xf /workspace/app/jmeter/target/jmeter.jar

# pre-compress the JMaaS catalogs
RUN mkdir -pv /workspace/catalogs/ \
    && find static/catalogs -name '*.json' \
    | xargs -I% sh -c 'gzip -9 -cvf % > /workspace/catalogs/$(basename %).gz'

FROM eclipse-temurin:17

ARG DEBIAN_FRONTEND=noninteractive

ARG DEPENDENCY=/workspace/app/target/dependency
ARG JMETER_VERSION="5.6.3"

ENV JMETER_HOME=/opt/apache-jmeter-${JMETER_VERSION}
ENV JMETER_BIN=${JMETER_HOME}/bin

VOLUME /tmp

COPY --from=build ${DEPENDENCY} /app
COPY --from=build /workspace/catalogs/*.json.gz /jmaas/catalogs/
COPY --from=ui /app/dist /app/static/ui

RUN apt-get -qq update  > /dev/null \
    && apt-get -qq -yy install ca-certificates > /dev/null \
    && apt-get -qq clean > /dev/null

RUN curl -sSL https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz > /tmp/jmeter.tgz \
    && mkdir -p /opt \
    && tar -xvf /tmp/jmeter.tgz -C /opt \
    && rm /tmp/jmeter.tgz \
    && ln -s /opt/apache-jmeter-${JMETER_VERSION} /opt/jmeter \
    && ln -s /opt/jmeter/bin/jmeter /usr/bin/jmeter

WORKDIR /opt/apache-jmeter-${JMETER_VERSION}/lib

RUN curl -sSL -O https://repo1.maven.org/maven2/kg/apc/cmdrunner/2.3/cmdrunner-2.3.jar

WORKDIR /opt/apache-jmeter-${JMETER_VERSION}/lib/ext/

RUN curl -sSL -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-manager/1.9/jmeter-plugins-manager-1.9.jar \
    && curl -sSL -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-casutg/3.0/jmeter-plugins-casutg-3.0.jar \
    && curl -sSL -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-tst/2.6/jmeter-plugins-tst-2.6.jar \
    && curl -sSL -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-functions/2.2/jmeter-plugins-functions-2.2.jar \
    && curl -sSL -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-httpraw/0.1/jmeter-plugins-httpraw-0.1.jar \
    && curl -sSL -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-common-io/0.2/jmeter-plugins-common-io-0.2.jar \
    && curl -sSL -O https://repo1.maven.org/maven2/com/github/marianobarrios/tls-channel/0.9.1/tls-channel-0.9.1.jar \
    && curl -sSL -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-fifo/0.2/jmeter-plugins-fifo-0.2.jar \
    && curl -sSL -O https://github.com/QAInsights/validate-thread-group/releases/download/v1.0.1/validatetg-1.0.1.jar \
    && curl -sSL -O https://repo1.maven.org/maven2/com/google/guava/guava/33.4.6-jre/guava-33.4.6-jre.jar \
    && java -cp jmeter-plugins-manager-1.9.jar org.jmeterplugins.repository.PluginManagerCMDInstaller

WORKDIR /opt/apache-jmeter-${JMETER_VERSION}/bin

RUN ./PluginsManagerCMD.sh install jpgc-casutg,jpgc-tst,jpgc-fifo,jpgc-functions,jpgc-json,jpgc-httpraw

# override httpraw plugin
COPY --from=build /workspace/app/jmeter/httpraw/target/jmeter-plugins-httpraw-0.1.jar /opt/apache-jmeter-${JMETER_VERSION}/lib/ext/

WORKDIR /

RUN mkdir -p /jmx/

COPY ./cloud_run.rc /.cloud_run.rc
COPY ./jmeter/jmaas.properties /jmaas.properties
COPY ./jmeter/src/main/jmeter/print_id_token /print_id_token
COPY ./jmeter/entrypoint.sh /entrypoint.sh
COPY ./jmeter/src/main/jmeter/*.jmx /jmx/

ENV PATH=$PATH:$JMETER_BIN

ENTRYPOINT ["/entrypoint.sh"]
