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

The server has two feature-equivalent implementations: **Java** (Docker/ZIP, also supports HTTP/SSE) and **Node.js** (MCPB Desktop extension). Both expose the same 6 tools with identical response formatting.

### Java

- **Language:** Java 25
- **Build:** Gradle 9.4.1 (Kotlin DSL)
- **HTTP server:** Jetty 12 (EE10)
- **MCP transport:** SSE (Server-Sent Events) via `HttpServletSseServerTransportProvider`
- **MCP SDK:** `io.modelcontextprotocol.sdk:mcp:1.1.0`
- **Main class:** `com.aon.tfl.App`
- **Tests:** JUnit 5 integration tests — spin up the full server on a random port and connect via an MCP client

### Node.js

- **Language:** TypeScript (compiled to ESM)
- **Runtime:** Node.js 22
- **Build:** `tsc` via npm scripts
- **MCP transport:** stdio via `@modelcontextprotocol/sdk` `StdioServerTransport`
- **Entry point:** `node/src/index.ts` → `node/dist/index.js`
- **Tests:** Vitest — unit tests use a mock HTTP server; contract tests hit the live TfL API
- **MCPB manifest:** `node/manifest.json`

## Development Workflow

We use **red-green TDD** and **Conventional Commits**:

### Commit Messages

Use focused, descriptive Conventional Commit messages. A good commit message:

- Uses an appropriate type and optional scope, such as `feat(java):`, `fix(node):`, `test:`, `docs:`, `refactor:`, or `chore:`
- Describes the user-visible or maintainer-visible change in the imperative mood
- Mentions the issue number when the work is tied to one, for example `feat(java): add line status filters (#123)`, or in the commit body
- Keeps each commit to one logical change so the message can be specific
- Includes a short body for most commits, explaining why the change exists and any important implementation or verification notes
- Omits the body only for very small, self-evident changes

Examples:

- `feat(java): expose service status MCP app resource`
- `refactor(node): load MCP app HTML from shared resources`
- `test(java): cover MCP app resource metadata (#123)`
- `docs: clarify release commit requirements`

### Java
1. Write a failing test in `AppTest.java` that covers the new tool
2. Implement the tool in `App.java` (minimal code to pass the test)
3. Refactor if needed
4. Commit using conventional commits and include the issue number in the subject or body when applicable (e.g., `feat(java): add arrivals filtering (#123)`) and push

### Node.js
1. Write a failing test in `node/test/server.test.ts` that covers the new tool
2. Implement the tool in `node/src/index.ts` (minimal code to pass the test)
3. Refactor if needed
4. Commit using conventional commits and include the issue number in the subject or body when applicable (e.g., `feat(node): add arrivals filtering (#123)`) and push

When adding a new tool, implement it in **both** Java and Node.js to keep the implementations in sync.

### Java Version Note

The project targets **Java 25** (`build.gradle.kts` toolchain). If the local environment only has an older JDK (e.g. Java 21) and you temporarily downgrade the toolchain to run tests, **you must restore it to Java 25 before committing**. The CI runners use Java 25 and the build will fail if `build.gradle.kts` specifies a lower version.

### Error Handling Patterns

The MCP tools must present errors gracefully to the calling LLM:
- Do not let exceptions crash the transport.
- Ensure both Java and Node.js implement the same tool-level error handling.
- Use standard MCP error responses (e.g., `isError: true` with a fallback `type: "text"` indicating what went wrong) when upstream TfL API calls fail.

### Adding a New Tool Checklist

1. **Implement in Java**: Add schema, write integration tests in `AppTest.java`, and implement in `App.java`.
2. **Implement in Node.js**: Add schema, write unit tests in `node/test/server.test.ts`, and implement in `node/src/index.ts`.
3. **Add Annotations**: Ensure the tool has either `readOnlyHint: true` or `destructiveHint: true`.
4. **Update Docs**: Add the new tool's description and parameters to `docs/tools.md`.

### Continuous Integration & PR Rules

- **Tests must pass**: Both `./gradlew test` and `npm test` must be passing before any merge.
- **Branching**: Commit against your designated feature branch (e.g., `claude/tdd-feature...`). Do not force-push to `main`.

### Running & Debugging Locally

- **Node.js**: Navigate to `node/`, run `npm run build`, and debug using the MCP Inspector: `npx @modelcontextprotocol/inspector node dist/index.js`
- **Java**: Point the MCP Inspector to the compiled jar or run `./gradlew test` with remote debugging attached if needed.

## Key Constraints

### Both implementations
- The TfL API key is optional for tests — stub or use a real key via `TFL_APP_KEY` env var
- All HTTP calls to TfL should respect the `TFL_APP_KEY` env var if set
- **Never commit API keys or secrets** — `TFL_APP_KEY` and `TFL_APP_ID` must only be supplied via environment variables or CI secrets, never hardcoded in source files, test fixtures, or committed configuration
- New tools must be added to both Java and Node.js implementations with identical response formatting

### Java
- Keep `App.java` simple; extract service/helper classes only when complexity justifies it
- Integration tests must pass: `./gradlew test`
- Tests use WireMock (`wiremock-jetty12:3.13.2`) to stub TfL API — no real network calls needed

### Node.js
- Keep `src/index.ts` as the single source file; extract modules only when complexity justifies it
- Unit tests must pass: `cd node && npm test`
- Tests use a mock HTTP server — no real network calls needed

## Working Branch

`claude/tdd-feature-development-XKlcZ`

## Feature Backlog

Pick up from the first unchecked item and follow the red-green TDD workflow above.

## Connectors Directory Submission Checklist

Tasks required to meet the [Anthropic Connectors Directory](https://docs.anthropic.com/en/docs/connectors/directory) submission standards. Pick up from the first unchecked item.

### Tool Annotations (Required)

All tools must include `readOnlyHint` or `destructiveHint`. Currently **none of the 6 tools** have these annotations.

- [x] Add `readOnlyHint: true` annotation to all 6 tools (`service_status`, `arrivals`, `journey`, `bike_points`, `crowding`, `fares`) — all are read-only queries against the TfL API
- [x] Add tests verifying tool annotations are present (query tool list via MCP client and assert hints exist)

### Privacy Policy (Required for local connectors)

No privacy policy currently exists. Required for MCPB/Desktop extension submission.

- [x] Add a "Privacy Policy" section to `README.md` covering: data collection practices, usage and storage, third-party sharing (TfL API), data retention, contact information
- [x] Add `privacy_policies` array with HTTPS URL(s) to `server.json` manifest (manifest_version 0.2+)
- [x] Host the privacy policy at a stable HTTPS URL (e.g. GitHub Pages or repo raw link)

### Authentication (Required — assess applicability)

OAuth 2.0 is required for authenticated services. TfL uses a simple API key model, not OAuth.

- [x] Determine submission category: submitting as a **Desktop extension (MCPB)** via stdio transport — no OAuth flow needed
- [x] Document that the TfL API key is optional and supplied via environment variable — no OAuth flow needed (documented in README Getting Started, Configuration, and `docs/api-keys.md`)

### Documentation & Support (Required)

Current docs are good but need enhancement for reviewer onboarding.

- [x] Add clear "Getting Started" / setup instructions to README aimed at a first-time reviewer unfamiliar with the project
- [x] Document all 6 tools with human-readable names and descriptions (current `docs/tools.md` is a good start — ensure it's comprehensive and matches submission form fields)
- [x] Provide a support channel link (e.g. GitHub Issues URL)
- [x] Prepare step-by-step test account instructions: how to get a TfL API key, configure the server, and verify it works (note: works without key at lower rate limits)

### Branding (Required)

- [x] Create a server logo (SVG preferred) — `docs/logo.svg` exists (roundel-inspired design); referenced in `server.json` icon field
- [x] Verify favicon is set if hosting a web presence — N/A: submitted as a Desktop extension (stdio), no web presence to favicon
- [x] Prepare promotional screenshot(s) showing the server in action with Claude — N/A: not required for Desktop extension submission

### Compliance & Policy (Required)

- [x] Review and confirm compliance with [Anthropic Software Directory Terms](https://support.claude.com/en/articles/13145338-anthropic-software-directory-terms)
- [x] Review and confirm compliance with [Anthropic Software Directory Policy](https://support.claude.com/en/articles/13145358-anthropic-software-directory-policy)
- [x] Confirm commitment to maintain security, respond to issues promptly, and provide accurate descriptions — see [COMPLIANCE.md](COMPLIANCE.md)

### Testing & Launch Readiness (Required)

- [x] Test the server in Claude Desktop (stdio transport) and verify all 6 tools work end-to-end — covered by StdioContractTest (automated)
- [x] Test the server as a remote MCP (HTTP/SSE transport) and verify connectivity — covered by AppTest (automated)
- [x] Ensure all unit tests pass: `./gradlew test`
- [x] Ensure contract tests pass: `./gradlew contractTest`
- [x] Confirm GA readiness and target launch date — GA date to be provided at submission time

### Submission Form Preparation

All content prepared in `docs/submission.md`.

- [x] Prepare server basics: name, URL, tagline, description, use cases
- [x] Prepare connection details: auth type, transport protocol, read/write capabilities
- [x] Prepare data & compliance info: data handling practices, third-party connections (TfL API), health data access (none), category (transport/travel)
- [x] Compile full tool list with human-readable names for submission form
- [x] Choose submission path: Desktop extension form (stdio/OCI package transport)
