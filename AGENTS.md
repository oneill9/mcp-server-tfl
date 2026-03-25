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

### Java Version Note

The project targets **Java 25** (`build.gradle.kts` toolchain). If the local environment only has an older JDK (e.g. Java 21) and you temporarily downgrade the toolchain to run tests, **you must restore it to Java 25 before committing**. The CI runners use Java 25 and the build will fail if `build.gradle.kts` specifies a lower version.

## Key Constraints

- Keep `App.java` simple; extract service/helper classes only when complexity justifies it
- Integration tests must pass: `./gradlew test`
- The TfL API key is optional for tests — stub or use a real key via `TFL_APP_KEY` env var
- All HTTP calls to TfL should respect the `TFL_APP_KEY` env var if set
- Tests use WireMock (`wiremock-jetty12:3.13.2`) to stub TfL API — no real network calls needed
- **Never commit API keys or secrets** — `TFL_APP_KEY` and `TFL_APP_ID` must only be supplied via environment variables or CI secrets, never hardcoded in source files, test fixtures, or committed configuration

## Working Branch

`claude/tdd-feature-development-XKlcZ`

## Feature Backlog

Pick up from the first unchecked item and follow the red-green TDD workflow above.

## Connectors Directory Submission Checklist

Tasks required to meet the [Anthropic Connectors Directory](https://docs.anthropic.com/en/docs/connectors/directory) submission standards. Pick up from the first unchecked item.

### Tool Annotations (Required)

All tools must include `readOnlyHint` or `destructiveHint`. Currently **none of the 9 tools** have these annotations.

- [ ] Add `readOnlyHint: true` annotation to all 9 tools (`line_status`, `arrivals`, `stop_search`, `disruptions`, `journey`, `bike_points`, `list_modes`, `air_quality`, `road_disruptions`) — all are read-only queries against the TfL API
- [ ] Add tests verifying tool annotations are present (query tool list via MCP client and assert hints exist)

### Privacy Policy (Required for local connectors)

No privacy policy currently exists. Required for MCPB/Desktop extension submission.

- [ ] Add a "Privacy Policy" section to `README.md` covering: data collection practices, usage and storage, third-party sharing (TfL API), data retention, contact information
- [ ] Add `privacy_policies` array with HTTPS URL(s) to `server.json` manifest (manifest_version 0.2+)
- [ ] Host the privacy policy at a stable HTTPS URL (e.g. GitHub Pages or repo raw link)

### Authentication (Required — assess applicability)

OAuth 2.0 is required for authenticated services. TfL uses a simple API key model, not OAuth.

- [ ] Determine submission category: if submitting as a **remote MCP**, assess whether OAuth 2.0 is needed for the server's own client auth (separate from TfL API key)
- [ ] If submitting as **Desktop extension (MCPB)** only, document that the TfL API key is optional and supplied via environment variable — no OAuth flow needed

### Documentation & Support (Required)

Current docs are good but need enhancement for reviewer onboarding.

- [ ] Add clear "Getting Started" / setup instructions to README aimed at a first-time reviewer unfamiliar with the project
- [ ] Document all 9 tools with human-readable names and descriptions (current `docs/tools.md` is a good start — ensure it's comprehensive and matches submission form fields)
- [ ] Provide a support channel link (e.g. GitHub Issues URL)
- [ ] Prepare step-by-step test account instructions: how to get a TfL API key, configure the server, and verify it works (note: works without key at lower rate limits)

### Branding (Required)

No branding assets currently exist.

- [ ] Create a server logo (SVG preferred) — e.g. a TfL roundel-inspired icon (be mindful of TfL trademark)
- [ ] Verify favicon is set if hosting a web presence
- [ ] Prepare promotional screenshot(s) showing the server in action with Claude

### Compliance & Policy (Required)

- [ ] Review and confirm compliance with [Anthropic Software Directory Terms](https://support.claude.com/en/articles/13145338-anthropic-software-directory-terms)
- [ ] Review and confirm compliance with [Anthropic Software Directory Policy](https://support.claude.com/en/articles/13145358-anthropic-software-directory-policy)
- [ ] Confirm commitment to maintain security, respond to issues promptly, and provide accurate descriptions

### Testing & Launch Readiness (Required)

- [ ] Test the server in Claude Desktop (stdio transport) and verify all 9 tools work end-to-end
- [ ] Test the server as a remote MCP (HTTP/SSE transport) and verify connectivity
- [ ] Ensure all unit tests pass: `./gradlew test`
- [ ] Ensure contract tests pass: `./gradlew contractTest`
- [ ] Confirm GA readiness and target launch date

### Submission Form Preparation

- [ ] Prepare server basics: name, URL, tagline, description, use cases
- [ ] Prepare connection details: auth type, transport protocol, read/write capabilities
- [ ] Prepare data & compliance info: data handling practices, third-party connections (TfL API), health data access (none), category (transport/travel)
- [ ] Compile full tool list with human-readable names for submission form
- [ ] Choose submission path: [Desktop extension form](http://clau.de/desktop-extention-submission) or [Remote MCP form](http://clau.de/mcp-directory-submission)
