FROM openjdk:11-jre-slim
RUN mkdir -p /usr/src/app/config/
COPY ./target/workflow-service-0.0.1-SNAPSHOT.jar /usr/src/app/app.jar

WORKDIR /usr/src/app

ENTRYPOINT ["java", "-jar", "app.jar"]
