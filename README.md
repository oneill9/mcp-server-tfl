# TFL MCP Server

[![Build](https://github.com/oneill9/tfl/actions/workflows/build.yml/badge.svg)](https://github.com/oneill9/tfl/actions/workflows/build.yml)

An MCP (Model Context Protocol) server that exposes the [TfL (Transport for London) Unified API](https://api.tfl.gov.uk/) as tools, allowing AI assistants like Claude to query live London transport data.

Built with Java 25, Gradle 9.4.1, Jetty 12, and the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) v1.1.0 (SSE transport).

## Tools

| Tool | Description |
|------|-------------|
| `echo` | Echoes back the provided text (dev/debug) |
| `greet` | Returns a greeting for the given name (dev/debug) |

> More tools covering TfL line status, arrivals, stop lookup, journey planning, and disruptions are planned.

## Configuration

| Environment Variable | Default | Description |
|----------------------|---------|-------------|
| `PORT` | `3001` | HTTP port the server listens on |
| `TFL_APP_KEY` | *(none)* | TfL API key — register at [api-portal.tfl.gov.uk](https://api-portal.tfl.gov.uk/) |

Requests work without an API key but are rate-limited. An app key raises the limit significantly.

## Running

```sh
TFL_APP_KEY=your_key_here ./gradlew run
```

Server starts on `http://localhost:3001` by default.

SSE endpoint: `/sse`
Message endpoint: `/mcp/message`

## Testing

```sh
./gradlew test
```

## TfL API Reference

- Unified API: <https://api.tfl.gov.uk/>
- API Portal / Key Registration: <https://api-portal.tfl.gov.uk/>
- Swagger UI: <https://api.tfl.gov.uk/swagger/ui/index.html>
