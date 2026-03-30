# TfL MCP Server

<img src="docs/logo.svg" alt="TfL MCP Server logo" width="80" align="right"/>

> **Disclaimer:** This is a community-built project. It is not affiliated with, endorsed by, or connected to Transport for London (TfL). This project consumes the publicly available [TfL Unified API](https://api.tfl.gov.uk/).

[![Build](https://github.com/oneill9/mcp-server-tfl/actions/workflows/build.yml/badge.svg)](https://github.com/oneill9/mcp-server-tfl/actions/workflows/build.yml)
[![Contract Tests](https://github.com/oneill9/mcp-server-tfl/actions/workflows/contract-tests.yml/badge.svg)](https://github.com/oneill9/mcp-server-tfl/actions/workflows/contract-tests.yml)
[![GitHub release](https://img.shields.io/github/v/release/oneill9/mcp-server-tfl)](https://github.com/oneill9/mcp-server-tfl/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A MCP server that exposes the [TfL (Transport for London) Unified API](https://api.tfl.gov.uk/) as tools, allowing AI assistants like Claude to query live London transport data.

Available in two implementations:
- **Node.js (MCPB)** — lightweight Desktop extension, recommended for Claude Desktop
- **Java** — Docker image or standalone ZIP, also supports HTTP/SSE transport

## Getting Started

**Step 1 — Add to Claude Desktop**

Open your Claude Desktop config file:
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`
- Linux: `~/.config/Claude/claude_desktop_config.json`

**Option A: Node.js MCPB (recommended)**

Download `tfl.mcpb` from [GitHub Releases](https://github.com/oneill9/mcp-server-tfl/releases) and install it in Claude Desktop, or add manually:

```json
{
  "mcpServers": {
    "tfl": {
      "command": "npx",
      "args": ["-y", "@oneill9/mcp-server-tfl"]
    }
  }
}
```

**Option B: Docker**

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
"env": { "TFL_APP_KEY": "your_key_here" }
```

**Step 3 — Restart Claude Desktop and start asking questions**

Example: *"Is the Central line running normally?"*, *"When is the next bus from Oxford Circus?"*

For full setup details (Java direct, Docker options), see [docs/installation.md](docs/installation.md).

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
| `line_routes` | Ordered sequence of stops along a line | `GET /Line/{id}/Route/Sequence/{direction}` |
| `crowding` | Live station crowding level | `GET /Crowding/{naptan}/Live` |
| `fares` | Fare information between two stops | `GET /StopPoint/{id}/FareTo/{targetId}` |

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

Both implementations use **stdio transport** — JSON-RPC over stdin/stdout, the standard MCP transport for Claude Desktop.

```sh
# Node.js
cd node && npm run build && node dist/index.js

# Java
./gradlew run
```

For use with Claude Desktop, see [docs/installation.md](docs/installation.md).

## Testing

### Node.js

Unit tests use a mock HTTP server — no network access or API key required:

```sh
cd node && npm test
```

Contract tests call the live TfL API:

```sh
cd node && TFL_APP_KEY=your_key_here npm run contractTest
```

### Java

Unit tests use WireMock to stub the TfL API — no network access or API key required:

```sh
./gradlew test
```

Contract tests spin up the server as a real subprocess and call the live TfL API:

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
