FROM openjdk:21-jdk-bullseye

WORKDIR .

COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
