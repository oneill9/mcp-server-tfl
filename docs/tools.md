# Tools Reference

The TfL MCP Server exposes 6 dynamic tools that query live London transport data via the [TfL Unified API](https://api.tfl.gov.uk/). 
Names of stops are automatically resolved to NaPTAN IDs dynamically.

---

## `service_status` — Service Status

Get the current operational status and disruptions of TfL transport modes.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `modes` | string | Yes | Comma-separated modes, e.g. `tube,bus,dlr` |

**Example query:** *"Is the Central line running normally?"*

**Example response (text fallback):**
```
Central: Good Service
Victoria: Minor Delays — Earlier signal failure at Stockwell
```

### MCP Apps UI (Progressive Enhancement)

This tool includes MCP Apps support. UI-capable hosts (e.g. Claude Desktop) can render an interactive service status board. Non-UI hosts continue to receive the text response above.

- **Resource URI:** `ui://tfl/service-status`
- **MIME type:** `text/html;profile=mcp-app`
- **Design:** London transport status-board inspired (uses standard line colours, not TfL brand identity)

The tool also returns structured JSON data alongside the text, allowing the UI to render status information without parsing human-readable text:

```json
[
  { "id": "central", "name": "Central", "statuses": [{ "severity": "Good Service" }] },
  { "id": "victoria", "name": "Victoria", "statuses": [{ "severity": "Minor Delays", "reason": "Earlier signal failure" }] }
]
```

---

## `arrivals` — Stop Arrivals

Get live arrival predictions at a TfL stop by its common name.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `stopName` | string | Yes | Stop name or search term, e.g. `oxford` |

**Example query:** *"When is the next tube from Oxford Circus?"*

**Example response:**
```
Central → Epping: 2 min (Eastbound - Platform 2)
Central → Epping: 5 min (Eastbound - Platform 2)
```

---

## `journey` — Journey Planner

Plan a journey between two points using the TfL Journey Planner, combining different transport modes.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `from` | string | Yes | Origin: NaPTAN ID, postcode, or `lat,lon` |
| `to` | string | Yes | Destination: NaPTAN ID, postcode, or `lat,lon` |

**Example query:** *"How do I get from King's Cross to Canary Wharf?"*

**Example response:**
```
Journey 1 (32 min):
  - Take Piccadilly line to King's Cross St. Pancras (2 min)
  - Take Jubilee line to Canary Wharf (28 min)
  - Walk to destination (2 min)
```

---

## `bike_points` — Santander Cycles Docking Stations

List all Santander Cycles docking stations across London with current bike and empty dock availability.

**Parameters:** None

**Example query:** *"Are there any bikes available near Clerkenwell?"*

**Example response:**
```
BikePoints_1 — River Street, Clerkenwell: 9 bikes, 9 empty docks
BikePoints_2 — Phillimore Gardens, Kensington: 0 bikes, 13 empty docks
```

---

## `crowding` — Station Crowding

Get live crowding data for a TfL station by its name, showing how busy it is compared to the typical baseline.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `stopName` | string | Yes | Stop name or search term, e.g. `oxford circus` |

**Example query:** *"How busy is Oxford Circus right now?"*

**Example response:**
```
940GZZLUOXC: 69% of typical crowding level (baseline ratio: 0.6863)
```

---

## `fares` — Fare Finder

Get fare information between two named TfL stops, including pay-as-you-go and cash single prices.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `fromName` | string | Yes | Origin stop name, e.g. `oxford circus` |
| `toName` | string | Yes | Destination stop name, e.g. `bond street` |

**Example query:** *"How much is a tube fare from Oxford Circus to Bond Street?"*

**Example response:**
```
Oxford Circus → Bond Street (Adult):
  £2.80 — Pay as you go (Peak)
  £2.70 — Pay as you go (Off Peak)
  £6.70 — CashSingle (Anytime)
```
