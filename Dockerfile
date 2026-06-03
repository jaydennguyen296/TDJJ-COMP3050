# Production image for Kubernetes (and standalone docker run).
# Dev container workflow continues to use .devcontainer/Dockerfile.

FROM eclipse-temurin:18-jdk AS build
WORKDIR /build
COPY src/main/java/comp3050 ./comp3050
COPY src/main/resources/map.txt ./
RUN javac comp3050/*.java comp3050/server/*.java

FROM eclipse-temurin:18-jre
RUN apt-get update \
    && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/comp3050/*.class ./comp3050/
COPY --from=build /build/comp3050/server/*.class ./comp3050/server/
COPY --from=build /build/map.txt ./
EXPOSE 8000
CMD ["java", "comp3050.server.Server"]

