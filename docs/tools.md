# Tools Reference

The TfL MCP Server exposes 10 read-only tools that query live London transport data via the [TfL Unified API](https://api.tfl.gov.uk/).

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

## `line_routes` — Line Route Sequence

Get the ordered sequence of stops along a TfL line in a given direction.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `lineId` | string | Yes | Line ID, e.g. `central`, `victoria`, `northern` |
| `direction` | string | Yes | Direction of travel: `inbound` or `outbound` |

**Example query:** *"What are all the stops on the Central line?"*

**Example response:**
```
Central (outbound):
  Epping Underground Station (940GZZLUEPG)
  Theydon Bois Underground Station (940GZZLUTHB)
  Oxford Circus Underground Station (940GZZLUOXC)
```

---

## `crowding` — Station Crowding

Get live crowding data for a TfL station, showing how busy it is compared to the typical baseline.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `naptan` | string | Yes | NaPTAN station ID, e.g. `940GZZLUOXC` |

**Example query:** *"How busy is Oxford Circus right now?"*

**Example response:**
```
940GZZLUOXC: 69% of typical crowding level (baseline ratio: 0.6863)
```

---

## `fares` — Fare Finder

Get fare information between two TfL stops, including pay-as-you-go and cash single prices.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `fromStopId` | string | Yes | Origin NaPTAN stop ID, e.g. `940GZZLUOXC` |
| `toStopId` | string | Yes | Destination NaPTAN stop ID, e.g. `940GZZLUBND` |

**Example query:** *"How much is a tube fare from Oxford Circus to Bond Street?"*

**Example response:**
```
Oxford Circus → Bond Street (Adult):
  £2.80 — Pay as you go (Peak)
  £2.70 — Pay as you go (Off Peak)
  £6.70 — CashSingle (Anytime)
```
