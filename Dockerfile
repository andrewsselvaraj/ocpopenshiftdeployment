# One Dockerfile for every service:  docker build --build-arg SERVICE=auth-service -t auth-service .
# Both stages use Red Hat UBI images, which run as a non-root user and accept the random UID
# OpenShift assigns under the restricted-v2 SCC.

ARG SERVICE=auth-service

# ---- build stage -------------------------------------------------------------------------
FROM registry.access.redhat.com/ubi9/openjdk-17:latest AS build
ARG SERVICE
WORKDIR /home/default/build
COPY --chown=185:0 .mvn/ .mvn/
COPY --chown=185:0 mvnw pom.xml ./
COPY --chown=185:0 common/pom.xml common/
COPY --chown=185:0 auth-service/pom.xml auth-service/
COPY --chown=185:0 org-service/pom.xml org-service/
COPY --chown=185:0 question-service/pom.xml question-service/
COPY --chown=185:0 exam-service/pom.xml exam-service/
COPY --chown=185:0 common/src common/src
COPY --chown=185:0 ${SERVICE}/src ${SERVICE}/src
# Tests run in CI before the image is built; the image build only packages.
RUN chmod +x mvnw && ./mvnw -B -q -pl ${SERVICE} -am package -DskipTests \
 && java -Djarmode=tools -jar ${SERVICE}/target/${SERVICE}.jar extract --layers --launcher --destination extracted

# ---- runtime stage -----------------------------------------------------------------------
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:latest
ARG SERVICE
LABEL org.opencontainers.image.title="${SERVICE}" \
      org.opencontainers.image.source="https://github.com/andrewsselvaraj/ocpopenshiftdeployment" \
      io.openshift.expose-services="8080:http,8081:management"
WORKDIR /deployments
# Layered copy: dependencies change rarely, application classes often.
COPY --from=build /home/default/build/extracted/dependencies/ ./
COPY --from=build /home/default/build/extracted/spring-boot-loader/ ./
COPY --from=build /home/default/build/extracted/snapshot-dependencies/ ./
COPY --from=build /home/default/build/extracted/application/ ./
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080 8081
USER 185
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
