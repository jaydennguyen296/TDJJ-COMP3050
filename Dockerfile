# Production image for Kubernetes (and standalone docker run).
# Dev container workflow continues to use .devcontainer/Dockerfile.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src/ src/
RUN mvn package -q -DskipTests \
    dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/target/lib/ lib/
EXPOSE 8000
CMD ["java", "-cp", "app.jar:lib/*", "comp3050.server.Server"]
