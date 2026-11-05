# Stage 1: Build application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify

# Optional database test image: supply DB_URL and credentials at runtime.
FROM build AS database-tests
RUN --mount=type=cache,target=/root/.m2 cp -a /root/.m2 /opt/maven-cache
ENTRYPOINT ["mvn", "-o", "-B", "-ntp", "-Dmaven.repo.local=/opt/maven-cache/repository", "test"]

# Stage 2: Run application
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 animerch \
    && useradd --uid 10001 --gid animerch --no-create-home animerch \
    && mkdir -p /app/uploads /app/logs \
    && chown -R animerch:animerch /app

COPY --from=build /app/target/*.jar app.jar

USER animerch

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
