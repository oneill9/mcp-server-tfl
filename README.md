# TfL MCP Server

<img src="docs/logo.svg" alt="TfL MCP Server logo" width="80" align="right"/>

> **Disclaimer:** This is a community-built project. It is not affiliated with, endorsed by, or connected to Transport for London (TfL). This project consumes the publicly available [TfL Unified API](https://api.tfl.gov.uk/).

[![GitHub release](https://img.shields.io/github/v/release/oneill9/tfl-mcp-server)](https://github.com/oneill9/tfl-mcp-server/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An MCP server that exposes the [TfL (Transport for London) Unified API](https://api.tfl.gov.uk/) as tools, allowing AI assistants like Claude to query live London transport data.

Version 2 is a single Node.js 22 implementation using the MCP `2026-07-28` protocol revision. It supports stdio for local hosts, npm, and MCPB, plus stateless Streamable HTTP at `/mcp` for remote deployments.

> **Protocol compatibility:** Streamable HTTP is modern-only MCP `2026-07-28`. The stdio entry point also accepts 2025-era initialization for desktop hosts such as Codex, while modern stdio clients continue to negotiate `2026-07-28`. Legacy HTTP/SSE is not supported.

## Getting Started

**Step 1 — Add to Claude Desktop**

Open your Claude Desktop config file:
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`
- Linux: `~/.config/Claude/claude_desktop_config.json`

**Option A: Node.js MCPB (recommended)**

Download `tfl-mcp-server.mcpb` from [GitHub Releases](https://github.com/oneill9/tfl-mcp-server/releases) and install it in Claude Desktop.

**Option B: npm**

With Node.js 22 or later installed, point Claude Desktop at the published package:

```json
{
  "mcpServers": {
    "tfl-mcp-server": {
      "command": "npx",
      "args": ["-y", "@oneill9/tfl-mcp-server@2.0.0"],
      "env": { "TFL_APP_KEY": "your_key_here" }
    }
  }
}
```

**Option C: Docker**

```json
{
  "mcpServers": {
    "tfl-mcp-server": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "-e", "TFL_APP_KEY", "ghcr.io/oneill9/tfl-mcp-server:latest"]
    }
  }
}
```

**Step 2 — Add a TfL API key (recommended)**

Without a key, TfL applies strict rate limits that will impact most real-world usage. [Register a free key](https://api-portal.tfl.gov.uk/) and pass it via the `TFL_APP_KEY` environment variable. The Docker example's `-e TFL_APP_KEY` forwards this value into the container:

```json
"env": { "TFL_APP_KEY": "your_key_here" }
```

**Step 3 — Restart Claude Desktop and start asking questions**

Example: *"Is the Central line running normally?"*, *"When is the next bus from Oxford Circus?"*

For source, Docker, and Streamable HTTP options, see [docs/installation.md](docs/installation.md).

## Tools

| Tool | Description | TfL Endpoint |
|------|-------------|--------------|
| `service_status` | Current status and disruptions by mode | `GET /Line/Mode/{modes}/Status` |
| `arrivals` | Live arrivals at a stop by name | `GET /StopPoint/{naptan}/Arrivals` |
| `journey` | Plan a journey between two points | `GET /Journey/JourneyResults/{from}/to/{to}` |
| `bike_points` | List Santander Cycles docking stations | `GET /BikePoint` |
| `crowding` | Live station crowding level by name | `GET /Crowding/{naptan}/Live` |
| `fares` | Fare information between two named stops | `GET /StopPoint/{id}/FareTo/{targetId}` |

> **Note:** The server uses the `/StopPoint/Search/{query}` endpoint internally to automatically resolve stop names to NaPTAN IDs for the `arrivals`, `crowding`, and `fares` tools.

### MCP Apps UI

The `service_status` tool includes [MCP Apps](https://modelcontextprotocol.io) support as a progressive enhancement. UI-capable hosts (e.g. Claude Desktop) can render an interactive service status board alongside the text response. Non-UI hosts continue to receive the standard text output.

- The UI is self-contained (inline HTML/CSS, no external assets)
- Structured JSON data is returned alongside the text fallback
- No additional setup is required — host support is detected automatically

See [docs/tools.md](docs/tools.md) for full details.

## Authentication

The MCPB and default container mode use stdio transport. The optional Streamable HTTP mode does not implement OAuth or any other server-side authentication; deploy it only behind an appropriate trusted access layer.

The TfL API uses a simple API key (`TFL_APP_KEY`) that you supply as an environment variable. A key is **strongly recommended** — without one, TfL applies strict rate limits that will impact most real-world usage. Registration is free. No OAuth flow, login, or account beyond the TfL portal is required to use this MCP server.

## Configuration

| Environment Variable | Default | Description |
|----------------------|---------|-------------|
| `TFL_APP_KEY` | *(none)* | TfL API key — register at [api-portal.tfl.gov.uk](https://api-portal.tfl.gov.uk/) |
| `TFL_APP_ID` | *(none)* | TfL App ID — only needed for older API registrations that issued both an ID and key |
| `MCP_MAX_REQUEST_BODY_BYTES` | `1048576` | Maximum JSON request body size accepted by Streamable HTTP |
| `MCP_REQUEST_TIMEOUT_MS` | `30000` | Streamable HTTP request timeout in milliseconds |
| `MCP_HEADERS_TIMEOUT_MS` | `10000` | Streamable HTTP header timeout in milliseconds; must not exceed the request timeout |

An API key is strongly recommended — without one, TfL's strict rate limits will impact most real-world usage. Registration is free at [api-portal.tfl.gov.uk](https://api-portal.tfl.gov.uk/).

## Running from source

Node.js 22 or later is required. Install from the lockfile and build before starting the server.

```sh
cd node
npm ci
npm run build
node dist/index.js
```

That starts stdio transport. For stateless Streamable HTTP:

```sh
cd node
HOST=127.0.0.1 PORT=8080 node dist/index.js --http
```

Connect an MCP `2026-07-28` client to `http://127.0.0.1:8080/mcp`. Set `MCP_ALLOWED_HOSTS` to a comma-separated hostname allowlist when serving under other hostnames.

For use with Claude Desktop, see [docs/installation.md](docs/installation.md).

## Testing

Unit tests use a mock HTTP server — no network access or API key required:

```sh
cd node && npm test
```

The Streamable HTTP acceptance tests are included in `npm test`. Run the official frozen MCP `2026-07-28` requirement set separately (optional extension probes outside that set are not scored):

```sh
cd node && npm run test:conformance
```

The conformance fixture registers the real production TfL tools and resource through the shared modern-only HTTP adapter. It also adds test-only diagnostic tools, resources, prompts, and flows needed by the referee's hard-coded probes; those diagnostics are not exposed by the production entry point.

Contract tests call the live TfL API:

```sh
cd node && TFL_APP_KEY=your_key_here npm run contractTest
```

Build and smoke-test the packed npm CLI, MCPB, and container distributions with `bash scripts/test-distributions.sh`. The script skips container checks when Docker is unavailable.

## Support

For questions, bug reports, or feature requests, please open an issue on [GitHub Issues](https://github.com/oneill9/tfl-mcp-server/issues).

## Privacy Policy

This MCP server acts as a local proxy between your AI assistant and the [TfL Unified API](https://api.tfl.gov.uk/). It does not collect, store, or transmit any personal data beyond what is required to forward your queries to TfL.

- **Data collection:** No analytics or telemetry is collected. Limited operational diagnostics are written to standard error but are not persisted by the server; a host may retain them under its own logging policy.
- **Usage and storage:** Queries are forwarded to TfL in real time and responses are returned immediately. No query history or results are persisted.
- **Third-party sharing:** Requests are forwarded to the TfL Unified API (`api.tfl.gov.uk`). See [TfL's privacy policy](https://tfl.gov.uk/corporate/privacy-and-cookies/) for how TfL handles API usage data.
- **Data retention:** The server retains no data and holds no state between requests. Host-managed standard-error retention is outside the server's control.
- **Contact:** For privacy concerns, open an issue at <https://github.com/oneill9/tfl-mcp-server/issues>.

The full privacy policy is available at [PRIVACY.md](PRIVACY.md).

## Compliance

This server has been reviewed for compliance with the [Anthropic Software Directory Terms](https://support.claude.com/en/articles/13145338-anthropic-software-directory-terms) and [Anthropic Software Directory Policy](https://support.claude.com/en/articles/13145358-anthropic-software-directory-policy). See [COMPLIANCE.md](COMPLIANCE.md) for the full compliance statement and maintainer commitments.

## TfL API Reference

- Unified API: <https://api.tfl.gov.uk/>
- API Portal / Key Registration: <https://api-portal.tfl.gov.uk/>
- Swagger UI: <https://api.tfl.gov.uk/swagger/ui/index.html>
