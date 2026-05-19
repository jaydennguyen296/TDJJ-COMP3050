FROM eclipse-temurin:18-jdk
WORKDIR /workspace

RUN apt-get update && apt-get install -y --no-install-recommends \
    maven \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY . .
RUN javac /workspace/main/server/*.java
EXPOSE 8000
CMD ["java", "-cp", "/workspace/main/server", "Server"]

