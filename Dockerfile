FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src ./src

RUN chmod +x mvnw
RUN ./mvnw --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring && \
    mkdir -p /app/data/documentos && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring

EXPOSE 8080 10000

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:InitialRAMPercentage=50.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
