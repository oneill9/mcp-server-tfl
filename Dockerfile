# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app
COPY . .
RUN ./gradlew installDist --no-daemon

# Run stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/install/mcp-server-tfl /app

RUN chmod +x /app/bin/mcp-server-tfl

ENTRYPOINT ["/app/bin/mcp-server-tfl"]
