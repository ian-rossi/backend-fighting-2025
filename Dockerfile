## Stage 1: clone repo
FROM redhat/ubi9-minimal:9.5 AS clone
WORKDIR /repo
RUN microdnf update -y && \
  microdnf install -y git && \
  git clone https://github.com/ian-rossi/backend-fighting-2025.git /repo

## Stage 2: build with maven builder image with native capabilities
FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21 AS build
WORKDIR /code
USER quarkus
COPY --from=clone /repo/src /code/src
COPY --from=clone --chown=quarkus:quarkus /repo/.mvn /code/.mvn
COPY --from=clone --chown=quarkus:quarkus /repo/pom.xml /code/pom.xml
COPY --from=clone --chown=quarkus:quarkus --chmod=075 /repo/mvnw /code/mvnw

RUN ./mvnw -B org.apache.maven.plugins:maven-dependency-plugin:3.9.9:go-offline && \
  ./mvnw package -Dnative

## Stage 3 : create the docker final image
FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work/
COPY --from=build /code/target/*-runner /work/application

# set up permissions for user `1001`
RUN chmod 775 /work /work/application \
  && chown -R 1001 /work \
  && chmod -R "g+rwX" /work \
  && chown -R 1001:root /work

EXPOSE 8080
USER 1001

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
