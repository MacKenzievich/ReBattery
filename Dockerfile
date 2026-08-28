FROM mirror.gcr.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/ReBattery-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
