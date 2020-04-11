FROM openjdk:8-jdk-alpine
ENV DOCKERIZE_VERSION v0.6.1
VOLUME /tmp
COPY target/keycloak-setup-0.0.1-SNAPSHOT.jar app.jar
RUN apk --no-cache add curl \
    && curl -L https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz | tar -C /usr/local/bin -xzv
EXPOSE 8080
