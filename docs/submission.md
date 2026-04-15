# Anthropic Connectors Directory — Submission Form Content

Prepared content for the [Desktop extension submission form](http://clau.de/desktop-extention-submission).

---

## Server Basics

| Field | Value |
|-------|-------|
| **Name** | TfL MCP Server |
| **URL / Repository** | https://github.com/oneill9/mcp-server-tfl |
| **Tagline** | Community-built server — real-time London transport data via the TfL Unified API |
| **Description** | A community-built MCP server that wraps the Transport for London (TfL) Unified API, giving AI assistants like Claude live access to London transport data. Not affiliated with or endorsed by Transport for London. Ask about tube line status, bus arrivals, journey planning, Santander Cycles availability, road disruptions, air quality, and more — powered by the publicly available TfL API. |
| **Use cases** | Check whether a specific tube or bus line is running normally; get live arrival times at any London stop; plan a journey between two points; find the nearest Santander Cycles docking station; check for road disruptions on London's A-roads; monitor London air quality. |
| **Category** | Transport / Travel |

---

## Connection Details

| Field | Value |
|-------|-------|
| **Transport protocol** | stdio (Desktop extension / MCPB) |
| **Auth type** | None (no OAuth). Optional TfL API key via `TFL_APP_KEY` environment variable. |
| **Read / Write** | Read-only — all 6 tools are read-only queries against the TfL API. No write operations. |
| **Packages** | `tfl.mcpb` (Node.js MCPB — primary), `ghcr.io/oneill9/mcp-server-tfl:latest` (OCI / Docker) |

---

## Data & Compliance

| Field | Value |
|-------|-------|
| **Data collection** | None. The server does not collect, log, or store any user data. |
| **Data storage** | None. No query history or results are persisted between requests. |
| **Third-party connections** | TfL Unified API (`api.tfl.gov.uk`). Queries are forwarded to TfL in real time. See [TfL's privacy policy](https://tfl.gov.uk/corporate/privacy-and-cookies/). |
| **Health data access** | No. |
| **Data retention** | None. The server holds no state between requests. |
| **Privacy policy URL** | https://raw.githubusercontent.com/oneill9/mcp-server-tfl/main/PRIVACY.md |

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

These examples are verified by the project's contract tests (`StdioContractTest.java`), which spawn the server as a real subprocess over stdio transport — the same way Claude Desktop connects.

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

Rationale: the server is distributed as both a Node.js MCPB bundle (primary) and a Docker image, both invoked via stdio transport — the standard Desktop extension pattern. No OAuth or server-side infrastructure is needed.
