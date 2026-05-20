FROM eclipse-temurin:18-jdk
WORKDIR /workspace

RUN apt-get update && apt-get install -y --no-install-recommends \
    maven \
    git \
    curl \
    nginx \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/nginx/sites-enabled/default

COPY .devcontainer/nginx.conf /etc/nginx/conf.d/default.conf
COPY . .
RUN mvn -q compile
EXPOSE 80 8000
CMD ["bash"]

