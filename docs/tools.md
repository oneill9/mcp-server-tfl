# Tools Reference

> **Disclaimer:** This is an **community-built** project. It is **not affiliated with, endorsed by, or connected to Transport for London (TfL)** in any way.

The TfL MCP Server exposes 9 read-only tools that query live London transport data via the [TfL Unified API](https://api.tfl.gov.uk/).

---

## `line_status` — Line Status

Get the current operational status of one or more TfL lines.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `lines` | string | Yes | Comma-separated line IDs, e.g. `central,victoria,circle,dlr` |

**Example query:** *"Is the Central line running normally?"*

**Example response:**
```
Central: Good Service
Victoria: Minor Delays — Earlier signal failure at Stockwell
```

---

## `arrivals` — Stop Arrivals

Get live arrival predictions at a TfL stop. Use `stop_search` first to find the stop ID.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `stopId` | string | Yes | NaPTAN stop ID, e.g. `940GZZLUOXC` |

**Example query:** *"When is the next tube from Oxford Circus?"*

**Example response:**
```
Central → Epping: 2 min (Eastbound - Platform 2)
Central → Epping: 5 min (Eastbound - Platform 2)
```

---

## `stop_search` — Stop Search

Search for TfL stops by common name or search term.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `query` | string | Yes | Stop name or search term, e.g. `oxford` |

**Example query:** *"What is the stop ID for Oxford Circus?"*

**Example response:**
```
940GZZLUOXC — Oxford Circus Underground Station
490000173RC — Oxford Circus
```

---

## `disruptions` — Disruptions by Mode

Get current service disruptions for one or more TfL transport modes.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `modes` | string | Yes | Comma-separated transport modes, e.g. `tube,bus,dlr` |

**Example query:** *"Are there any tube disruptions right now?"*

**Example response:**
```
central: Minor delays due to earlier signal failure near Oxford Circus
jubilee: Good service
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

## `list_modes` — Transport Modes

Get a list of all valid TfL transport modes. Useful for building queries to `disruptions`.

**Parameters:** None

**Example query:** *"What transport modes does TfL support?"*

**Example response:**
```
tube, bus, dlr, overground, elizabeth-line, tflrail, national-rail, river-bus, cable-car, tram, cycle-hire
```

---

## `air_quality` — Air Quality

Get the latest London air quality data feed from TfL.

**Parameters:** None

**Example query:** *"What is the air quality like in London today?"*

**Example response:**
```json
{
  "forecastSummary": "Low pollution forecast for today..."
}
```

---

## `road_disruptions` — Road Disruptions

Get a list of current disruptions on streets and A-roads in London.

**Parameters:** None

**Example query:** *"Are there any road disruptions on the North Circular?"*

**Example response:**
```
A406 North Circular: Lane closed due to roadworks between Junction 1 and Junction 2
```
