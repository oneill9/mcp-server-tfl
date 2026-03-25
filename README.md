# TFL MCP Server

<img src="docs/logo.svg" alt="TfL MCP Server logo" width="80" align="right"/>

[![Build](https://github.com/oneill9/mcp-server-tfl/actions/workflows/build.yml/badge.svg)](https://github.com/oneill9/mcp-server-tfl/actions/workflows/build.yml)
[![Contract Tests](https://github.com/oneill9/mcp-server-tfl/actions/workflows/contract-tests.yml/badge.svg)](https://github.com/oneill9/mcp-server-tfl/actions/workflows/contract-tests.yml)
[![GitHub release](https://img.shields.io/github/v/release/oneill9/mcp-server-tfl)](https://github.com/oneill9/mcp-server-tfl/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An MCP (Model Context Protocol) server that exposes the [TfL (Transport for London) Unified API](https://api.tfl.gov.uk/) as tools, allowing AI assistants like Claude to query live London transport data.

Built with Java 25, Gradle 9.4.1, Jetty 12, and the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) v1.1.0 (SSE transport).

## Getting Started

The quickest way to use this server is via Docker — no Java installation required.

**Prerequisites:**
- [Docker](https://docs.docker.com/get-docker/) (recommended), **or** Java 25 + Gradle 9

**Step 1 — Add to Claude Desktop**

Open your Claude Desktop config file:
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

Add the server under `mcpServers`:

```json
{
  "mcpServers": {
    "tfl": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "ghcr.io/oneill9/mcp-server-tfl:latest"]
    }
  }
}
```

**Step 2 — (Optional) Add a TfL API key**

The server works without a key at low rate limits. For heavier use, [register a free key](https://api-portal.tfl.gov.uk/) and pass it via the `TFL_APP_KEY` environment variable:

```json
"args": ["run", "-i", "--rm", "-e", "TFL_APP_KEY=your_key_here", "ghcr.io/oneill9/mcp-server-tfl:latest"]
```

**Step 3 — Restart Claude Desktop and start asking questions**

Example: *"Is the Central line running normally?"*, *"When is the next bus from Oxford Circus?"*

For full setup details and the Java-direct option, see [docs/installation.md](docs/installation.md).

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

## Authentication

This server is distributed as a **Desktop extension (MCPB)** using stdio transport. It does not implement OAuth or any server-side authentication.

The TfL API uses a simple API key (`TFL_APP_KEY`) that you supply as an environment variable. The key is **optional** — the server works without one at lower rate limits. No OAuth flow, login, or account is required to use this MCP server.

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

Unit tests use WireMock to stub the TfL API — no network access or API key required:

```sh
./gradlew test
```

Contract tests spin up the server as a real subprocess (the same way Claude Desktop does) and call the live TfL API:

```sh
TFL_APP_KEY=your_key_here ./gradlew contractTest
```

## Support

For questions, bug reports, or feature requests, please open an issue on [GitHub Issues](https://github.com/oneill9/mcp-server-tfl/issues).

## Privacy Policy

This MCP server acts as a local proxy between your AI assistant and the [TfL Unified API](https://api.tfl.gov.uk/). It does not collect, store, or transmit any personal data beyond what is required to forward your queries to TfL.

- **Data collection:** No user data is collected or logged by this server.
- **Usage and storage:** Queries are forwarded to TfL in real time and responses are returned immediately. No query history or results are persisted.
- **Third-party sharing:** Requests are forwarded to the TfL Unified API (`api.tfl.gov.uk`). See [TfL's privacy policy](https://tfl.gov.uk/corporate/privacy-and-cookies/) for how TfL handles API usage data.
- **Data retention:** No data is retained. The server holds no state between requests.
- **Contact:** For privacy concerns, open an issue at <https://github.com/oneill9/mcp-server-tfl/issues>.

The full privacy policy is available at [PRIVACY.md](PRIVACY.md).

## Compliance

This server has been reviewed for compliance with the [Anthropic Software Directory Terms](https://support.claude.com/en/articles/13145338-anthropic-software-directory-terms) and [Anthropic Software Directory Policy](https://support.claude.com/en/articles/13145358-anthropic-software-directory-policy). See [COMPLIANCE.md](COMPLIANCE.md) for the full compliance statement and maintainer commitments.

## TfL API Reference

- Unified API: <https://api.tfl.gov.uk/>
- API Portal / Key Registration: <https://api-portal.tfl.gov.uk/>
- Swagger UI: <https://api.tfl.gov.uk/swagger/ui/index.html>
