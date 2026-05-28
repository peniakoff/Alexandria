# ─── Stage 1: build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY api ./api
COPY core ./core
COPY desktop ./desktop
COPY config ./config

RUN chmod +x gradlew && ./gradlew --no-daemon :api:installDist -x test

# ─── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

COPY --from=build /app/api/build/install/api/ ./

EXPOSE 8080
ENTRYPOINT ["/app/bin/api"]

