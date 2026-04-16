# Build context: repo root (currency-rate-proto is a sibling module dependency)
FROM eclipse-temurin:21-jdk-jammy AS build

RUN apt-get update \
 && apt-get install -y --no-install-recommends maven \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# POMs before source so dependency resolution is a cached layer.
COPY pom.xml                          ./pom.xml
COPY currency-rate-proto/pom.xml      ./currency-rate-proto/pom.xml
COPY currency-rate-provider/pom.xml   ./currency-rate-provider/pom.xml
COPY rate-printer/pom.xml             ./rate-printer/pom.xml

RUN mvn dependency:go-offline \
      -pl currency-rate-proto,currency-rate-provider -am \
      -DskipTests -DskipPactPublish --no-transfer-progress

COPY currency-rate-proto/src      ./currency-rate-proto/src
COPY currency-rate-provider/src   ./currency-rate-provider/src

RUN mvn package \
      -pl currency-rate-provider -am \
      -DskipTests -DskipPactPublish --no-transfer-progress

FROM eclipse-temurin:21-jre-jammy AS run

WORKDIR /app
COPY --from=build /workspace/currency-rate-provider/target/currency-rate-provider-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
