FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/rate-printer-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
