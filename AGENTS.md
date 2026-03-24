# AGENTS.md — TFL MCP Server

## Project Purpose

This is an **MCP (Model Context Protocol) server** that wraps the **TfL (Transport for London) Unified API**, making live London transport data accessible to AI assistants such as Claude.

The server exposes TfL capabilities as MCP tools so an assistant can answer questions like:
- "Is the Central line running normally?"
- "When is the next bus from stop ABC?"
- "How do I get from King's Cross to Canary Wharf right now?"
- "Are there any disruptions on the Elizabeth line?"

## TfL API

- Base URL: `https://api.tfl.gov.uk`
- Docs / Swagger: `https://api.tfl.gov.uk/swagger/ui/index.html`
- Key registration: `https://api-portal.tfl.gov.uk/`
- API key is passed as query param `app_key=<key>` or via `TFL_APP_KEY` env var
- No key required for low-volume usage; key raises rate limits

Key TfL API areas (each maps to one or more MCP tools):

| TfL API area | Example endpoint | Planned tool |
|---|---|---|
| Line status | `GET /Line/{ids}/Status` | `line_status` |
| Stop arrivals | `GET /StopPoint/{id}/Arrivals` | `arrivals` |
| Stop search | `GET /StopPoint/Search/{query}` | `stop_search` |
| Journey planner | `GET /Journey/JourneyResults/{from}/to/{to}` | `journey` |
| Disruptions | `GET /Line/Mode/{modes}/Disruption` | `disruptions` |
| Bike points | `GET /BikePoint` | `bike_points` |

## Architecture

- **Language:** Java 25
- **Build:** Gradle 9.4.1 (Kotlin DSL)
- **HTTP server:** Jetty 12 (EE10)
- **MCP transport:** SSE (Server-Sent Events) via `HttpServletSseServerTransportProvider`
- **MCP SDK:** `io.modelcontextprotocol.sdk:mcp:1.1.0`
- **Main class:** `com.aon.tfl.App`
- **Tests:** JUnit 5 integration tests — spin up the full server on a random port and connect via an MCP client

## Development Workflow

We use **red-green TDD**:
1. Write a failing test in `AppTest.java` that covers the new tool
2. Implement the tool in `App.java` (minimal code to pass the test)
3. Refactor if needed
4. Commit and push

## Key Constraints

- Keep `App.java` simple; extract service/helper classes only when complexity justifies it
- Integration tests must pass: `./gradlew test`
- The TfL API key is optional for tests — stub or use a real key via `TFL_APP_KEY` env var
- All HTTP calls to TfL should respect the `TFL_APP_KEY` env var if set
- Tests use WireMock (`wiremock-jetty12:3.13.2`) to stub TfL API — no real network calls needed

## Working Branch

`claude/tdd-feature-development-XKlcZ`

## Feature Backlog

Pick up from the first unchecked item and follow the red-green TDD workflow above.

- [x] `line_status` — current status of one or more lines (`GET /Line/{ids}/Status`)
- [ ] `arrivals` — live arrivals at a stop (`GET /StopPoint/{id}/Arrivals`)
- [ ] `stop_search` — search for stops by name/query (`GET /StopPoint/Search/{query}`)
- [ ] `disruptions` — current disruptions by mode (`GET /Line/Mode/{modes}/Disruption`)
- [ ] `journey` — plan a journey between two points (`GET /Journey/JourneyResults/{from}/to/{to}`)
- [ ] `bike_points` — list Santander Cycles docking stations (`GET /BikePoint`)
- [ ] Remove placeholder `echo` and `greet` tools once real tools are in place
- [ ] Update README tools table to reflect all implemented tools
