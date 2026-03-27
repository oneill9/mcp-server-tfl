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
| **Read / Write** | Read-only — all 12 tools are read-only queries against the TfL API. No write operations. |
| **Package** | `ghcr.io/oneill9/mcp-server-tfl:latest` (OCI / Docker) |

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
| `line_status` | Line Status | Get the current operational status of one or more TfL lines (e.g. tube, DLR, Overground). |
| `arrivals` | Stop Arrivals | Get live arrival predictions at any TfL stop by NaPTAN ID. |
| `stop_search` | Stop Search | Search for TfL stops by name or keyword. Returns NaPTAN IDs and coordinates. |
| `disruptions` | Disruptions by Mode | Get current service disruptions for one or more TfL transport modes (e.g. tube, bus, dlr). |
| `journey` | Journey Planner | Plan a journey between two points using the TfL Journey Planner, combining multiple modes. |
| `bike_points` | Santander Cycles Docking Stations | List all Santander Cycles docking stations with current bike and empty dock availability. |
| `list_modes` | Transport Modes | Get a list of all valid TfL transport mode identifiers for use in other tools. |
| `air_quality` | Air Quality | Get the latest London air quality forecast from TfL. |
| `road_disruptions` | Road Disruptions | Get current disruptions on London's streets and A-roads. |
| `line_routes` | Line Route Sequence | Get the ordered sequence of stops along a TfL line in a given direction. |
| `crowding` | Station Crowding | Get live crowding data for a TfL station as a percentage of the typical baseline. |
| `fares` | Fare Finder | Get fare information between two TfL stops, including pay-as-you-go and cash prices. |

---

## Working Use Case Examples

These examples are verified by the project's contract tests (`StdioContractTest.java`), which spawn the server as a real subprocess over stdio transport — the same way Claude Desktop connects.

### Example 1: Check tube line status

**Prompt:** "Is the Central line running normally?"

**Tool call:** `line_status` with `{"lines": "central"}`

**Expected output:** The Central line's current status (e.g. "Central: Good Service" or "Central: Minor Delays — reason…"). Verified by `lineStatusWithoutKey()`.

### Example 2: Search for a stop and get arrivals

**Prompt:** "When is the next bus from Oxford Circus?"

**Tool calls:**
1. `stop_search` with `{"query": "oxford circus"}` → returns stop IDs including Oxford Circus
2. `arrivals` with `{"stopId": "940GZZLUOXC"}` → returns live arrival predictions sorted by time

Verified by `stopSearchWithoutKey()` and `arrivalsWithoutKey()`.

### Example 3: Plan a journey

**Prompt:** "How do I get from Pimlico to Tower Bridge?"

**Tool call:** `journey` with `{"from": "51.4952,-0.1441", "to": "51.5179,-0.0816"}`

**Expected output:** One or more journey options with duration and step-by-step legs (e.g. "Journey 1 (25 min): Walk to Pimlico (3 min), Take Victoria line to Green Park (5 min)…"). Verified by `journeyWithoutKey()`.

### Example 4: Check air quality

**Prompt:** "What's the air quality like in London today?"

**Tool call:** `air_quality` with `{}`

**Expected output:** The latest London air quality forecast data feed from TfL. Verified by `airQualityWithoutKey()`.

---

## Submission Path

**Desktop extension form**: http://clau.de/desktop-extention-submission

Rationale: the server is distributed as a Docker image invoked via stdio transport — the standard Desktop extension (MCPB) pattern. No OAuth or server-side infrastructure is needed.
