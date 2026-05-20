# Production image for Kubernetes (and standalone docker run).
# Dev container workflow continues to use .devcontainer/Dockerfile.

FROM eclipse-temurin:18-jdk AS build
WORKDIR /build
COPY main/server/*.java main/server/map.txt ./
RUN javac *.java

FROM eclipse-temurin:18-jre
WORKDIR /app
COPY --from=build /build/*.class /build/map.txt ./
EXPOSE 8000
CMD ["java", "Server"]
