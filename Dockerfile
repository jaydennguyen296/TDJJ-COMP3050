# Production image for Kubernetes (and standalone docker run).

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn package -DskipTests
RUN mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency

FROM eclipse-temurin:17-jre
RUN apt-get update -o Acquire::Retries=3 \
    && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/* /usr/bin/pebble
WORKDIR /app
COPY --from=build /app/target/classes ./target/classes
COPY --from=build /app/target/dependency ./target/dependency
EXPOSE 8000
CMD ["java", "-cp", "target/classes:target/dependency/*", "comp3050.server.Server"]

