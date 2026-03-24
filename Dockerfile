# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . .
RUN ./gradlew installDist --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/install/mcp-server-tfl /app

# Ensure executable permissions
RUN chmod +x /app/bin/mcp-server-tfl

# Expose SSE port if needed by some clients, but MCP normally communicates over stdio
# EXPOSE 8080

ENTRYPOINT ["/app/bin/mcp-server-tfl"]
