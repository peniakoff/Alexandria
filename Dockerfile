# ─── Stage 1: build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --configuration runtimeClasspath -q

COPY src ./src
RUN ./gradlew --no-daemon jar -x test

# ─── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime
LABEL maintainer="peniakoff"
LABEL description="Alexandria – library management application"

WORKDIR /app

# JavaFX requires a display; for headless/container use add JAVA_OPTS as needed
COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/build/libs/ ./libs/

# Application config (override via env vars or mount application.properties)
ENV ALEXANDRIA_DATASOURCE_TYPE=MYSQL \
    ALEXANDRIA_DB_URL=jdbc:mysql://db:3306/alexandria?serverTimezone=UTC \
    ALEXANDRIA_DB_USERNAME=alexandria \
    ALEXANDRIA_DB_PASSWORD=changeme \
    ALEXANDRIA_DB_POOL_SIZE=5 \
    ALEXANDRIA_DB_MINIMUM_IDLE=1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
