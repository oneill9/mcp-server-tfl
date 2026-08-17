# Anthropic Connectors Directory — Submission Form Content

Prepared content for the [Desktop extension submission form](http://clau.de/desktop-extention-submission).

---

## Server Basics

| Field | Value |
|-------|-------|
| **Name** | TfL MCP Server |
| **URL / Repository** | https://github.com/oneill9/tfl-mcp-server |
| **Tagline** | Community-built server — real-time London transport data via the TfL Unified API |
| **Description** | A community-built MCP server that wraps the Transport for London (TfL) Unified API, giving AI assistants like Claude live access to London transport data. Not affiliated with or endorsed by Transport for London. Ask about service status, live arrivals, journey planning, Santander Cycles availability, station crowding, and fares — powered by the publicly available TfL API. |
| **Use cases** | Check whether a TfL service is running normally; get live arrival times at a London stop; plan a journey between two points; check Santander Cycles docking-station availability; check live station crowding; find fare information between two stops. |
| **Category** | Transport / Travel |

---

## Connection Details

| Field | Value |
|-------|-------|
| **Transport protocol** | MCP `2026-07-28`; stdio (Desktop extension / MCPB and default OCI mode) also accepts 2025-era desktop clients, plus modern-only stateless Streamable HTTP at `/mcp` |
| **Auth type** | None (no OAuth). Optional TfL API key via `TFL_APP_KEY` environment variable. |
| **Read / Write** | Read-only — all 6 tools are read-only queries against the TfL API. No write operations. |
| **Packages** | `tfl-mcp-server.mcpb` (Node.js MCPB — primary), `@oneill9/tfl-mcp-server` (npm / stdio), `ghcr.io/oneill9/tfl-mcp-server:latest` (OCI / Docker) |

---

## Data & Compliance

| Field | Value |
|-------|-------|
| **Data collection** | No analytics or telemetry. Limited operational diagnostics are written to standard error and are not persisted by the server. |
| **Data storage** | None by the server. No query history or results are persisted between requests; a host may retain standard-error output under its own logging policy. |
| **Third-party connections** | TfL Unified API (`api.tfl.gov.uk`). Queries are forwarded to TfL in real time. See [TfL's privacy policy](https://tfl.gov.uk/corporate/privacy-and-cookies/). |
| **Health data access** | No. |
| **Data retention** | None by the server. The server holds no state between requests. |
| **Privacy policy URL** | https://raw.githubusercontent.com/oneill9/tfl-mcp-server/main/PRIVACY.md |

---

## Full Tool List

| Tool name (ID) | Human-readable name | Description |
|----------------|---------------------|-------------|
| `service_status` | Service Status | Get the current operational status and disruptions for TfL transport modes (e.g. tube, dlr, overground). |
| `arrivals` | Stop Arrivals | Get live arrival predictions at any TfL stop by searching its name. |
| `journey` | Journey Planner | Plan a journey between two points using the TfL Journey Planner, combining multiple modes. |
| `bike_points` | Santander Cycles Docking Stations | List all Santander Cycles docking stations with current bike and empty dock availability. |
| `crowding` | Station Crowding | Get live crowding data for a TfL station by name as a percentage of the typical baseline. |
| `fares` | Fare Finder | Get fare information between two named TfL stops, including pay-as-you-go and cash prices. |

---

## Working Use Case Examples

These examples are verified by `node/test/contract.test.ts`, which spawns the built server as a real subprocess over stdio — the same way Claude Desktop connects. `node/test/http.test.ts` verifies the stateless Streamable HTTP transport. The official conformance fixture exercises the production TfL registrations and shared modern-only HTTP adapter while adding test-only diagnostics required by the referee's hard-coded probes; those diagnostics are never exposed by production.

### Example 1: Check tube line status

**Prompt:** "Is the Central line running normally?"

**Tool call:** `service_status` with `{"modes": "tube"}`

**Expected output:** The Central line's current status (e.g. "Central: Good Service" or "Central: Minor Delays — reason…"). Verified by `serviceStatusWithoutKey()`.

### Example 2: Get arrivals for a stop

**Prompt:** "When is the next bus from Oxford Circus?"

**Tool call:**
1. `arrivals` with `{"stopName": "oxford circus"}` → internally resolves the name and returns live arrival predictions sorted by time.

Verified by `arrivalsWithoutKey()`.

### Example 3: Plan a journey

**Prompt:** "How do I get from Pimlico to Tower Bridge?"

**Tool call:** `journey` with `{"from": "51.4952,-0.1441", "to": "51.5179,-0.0816"}`

**Expected output:** One or more journey options with duration and step-by-step legs (e.g. "Journey 1 (25 min): Walk to Pimlico (3 min), Take Victoria line to Green Park (5 min)…"). Verified by `journeyWithoutKey()`.



## Submission Path

**Desktop extension form**: http://clau.de/desktop-extention-submission

Rationale: the server is distributed as a Node.js MCPB bundle (primary) and an OCI image. Both support stdio, the standard Desktop extension pattern; the OCI image also supports opt-in stateless Streamable HTTP at `/mcp`. No OAuth is needed for the local MCPB submission path.
