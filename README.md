# TFL MCP Server

[![Build](https://github.com/oneill9/mcp-server-tfl/actions/workflows/build.yml/badge.svg)](https://github.com/oneill9/mcp-server-tfl/actions/workflows/build.yml)

An MCP (Model Context Protocol) server that exposes the [TfL (Transport for London) Unified API](https://api.tfl.gov.uk/) as tools, allowing AI assistants like Claude to query live London transport data.

Built with Java 25, Gradle 9.4.1, Jetty 12, and the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) v1.1.0 (SSE transport).

## Tools

| Tool | Description | TfL Endpoint |
|------|-------------|--------------|
| `line_status` | Current status of one or more lines | `GET /Line/{ids}/Status` |
| `arrivals` | Live arrivals at a stop | `GET /StopPoint/{id}/Arrivals` |
| `stop_search` | Search for stops by name/query | `GET /StopPoint/Search/{query}` |
| `disruptions` | Current disruptions by transport mode | `GET /Line/Mode/{modes}/Disruption` |
| `journey` | Plan a journey between two points | `GET /Journey/JourneyResults/{from}/to/{to}` |
| `bike_points` | List Santander Cycles docking stations | `GET /BikePoint` |
| `list_modes` | Get a list of all valid TfL transport modes | `GET /Line/Meta/Modes` |
| `air_quality` | Get the latest London air quality data feed | `GET /AirQuality` |
| `road_disruptions` | Get a list of disrupted streets and A-roads | `GET /Road/all/Street/Disruption` |

## Configuration

| Environment Variable | Default | Description |
|----------------------|---------|-------------|
| `TFL_APP_KEY` | *(none)* | TfL API key — register at [api-portal.tfl.gov.uk](https://api-portal.tfl.gov.uk/) |
| `TFL_APP_ID` | *(none)* | TfL App ID — only needed for older API registrations that issued both an ID and key |

Requests work without an API key but are rate-limited. An app key raises the limit significantly.

## Running

The server uses **stdio transport** — it reads JSON-RPC from stdin and writes responses to stdout, which is the standard MCP transport for Claude Desktop.

```sh
TFL_APP_KEY=your_key_here ./gradlew run
```

For use with Claude Desktop, see [docs/installation.md](docs/installation.md).

## Testing

```sh
./gradlew test
```

## TfL API Reference

- Unified API: <https://api.tfl.gov.uk/>
- API Portal / Key Registration: <https://api-portal.tfl.gov.uk/>
- Swagger UI: <https://api.tfl.gov.uk/swagger/ui/index.html>
