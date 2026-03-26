FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/currency-rate-provider-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
