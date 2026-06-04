# Production image for Kubernetes (and standalone docker run).

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update -o Acquire::Retries=3 \
    && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/* /usr/bin/pebble
WORKDIR /app
COPY --from=build /app/target/classes ./target/classes
EXPOSE 8000
CMD ["java", "-cp", "target/classes", "comp3050.server.Server"]

