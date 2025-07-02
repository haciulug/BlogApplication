FROM openjdk:17-jdk-slim

LABEL maintainer="haci.ulug@scalefocus.com"

ARG PORT=8080
ENV APP_PORT=$PORT
EXPOSE $APP_PORT

# active profile
ARG PROFILE=h2
ENV SPRING_PROFILES_ACTIVE=$PROFILE

ARG JAR_FILE=target/*.jar

ADD ${JAR_FILE} app.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar", "--server.port=${APP_PORT}"]