FROM eclipse-temurin:17-jdk-alpine as build
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

COPY gcloud gcloud

RUN --mount=type=cache,target=/root/.m2 ./mvnw -Pgcloud clean install -DskipTests
RUN mkdir -p target/dependency

WORKDIR /workspace/app/target/dependency

RUN jar -xf /workspace/app/gcloud/target/gcloud.jar

FROM eclipse-temurin:17

ARG DEPENDENCY=/workspace/app/target/dependency
ARG GOOGLE_CLOUD_SDK_VERSION='509.0.0'

VOLUME /tmp

COPY --from=build ${DEPENDENCY} /app

ENV CLOUDSDK_HOME /google-cloud-sdk
ENV	CLOUDSDK_BIN ${CLUDSDK_HOME}/bin

ENV GOOGLE_CLOUD_SDK_HOME ${CLOUDSDK_HOME}
ENV	GOOGLE_CLOUD_SDK_BIN ${CLOUDSDK_HOME}/bin

RUN wget -nv -q -P / \
  "https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-${GOOGLE_CLOUD_SDK_VERSION}-linux-x86_64.tar.gz" \
  && tar -xzf /google-cloud-cli-${GOOGLE_CLOUD_SDK_VERSION}-linux-x86_64.tar.gz -C / \
  && rm -vf /google-cloud-cli-${GOOGLE_CLOUD_SDK_VERSION}-linux-x86_64.tar.gz \
  && /google-cloud-sdk/bin/gcloud components update --quiet \
  && ln -s /google-cloud-sdk/bin/* /usr/bin/ \
  && echo "export PATH=$PATH:/google-cloud-sdk/bin" >> ~/.bashrc \
  && export PATH="$PATH:/google-cloud-sdk/bin"

ENV PATH $PATH:${GOOGLE_CLOUD_SDK_BIN}

COPY ./gcloud/gcloud-command-runner.properties /gcloud-command-runner.properties
COPY ./gcloud/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
