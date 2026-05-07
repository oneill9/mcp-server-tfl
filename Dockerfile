# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app
COPY . .
RUN ./gradlew installDist --no-daemon

# Run stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/install/tfl-mcp-server /app

RUN chmod +x /app/bin/tfl-mcp-server \
    && addgroup -S app && adduser -S app -G app

LABEL io.modelcontextprotocol.server.name="io.github.oneill9/tfl-mcp-server"

USER app

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD test -d /proc/1 || exit 1

ENTRYPOINT ["/app/bin/tfl-mcp-server"]
