# TFL

[![Build](https://github.com/oneill9/tfl/actions/workflows/build.yml/badge.svg)](https://github.com/oneill9/tfl/actions/workflows/build.yml)

MCP (Model Context Protocol) server built with Java 25, Gradle 9.4.1, and Jetty 12.

Uses the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) v1.1.0 with SSE transport.

## Tools

| Tool | Description |
|------|-------------|
| `echo` | Echoes back the provided text |
| `greet` | Returns a greeting for the given name |

## Running

```sh
./gradlew run
```

Server starts on `http://localhost:3001` by default. Override with `PORT` env var.

## Testing

```sh
./gradlew test
```
