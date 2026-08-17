## What's Changed in v2.0.0

Version 2 migrates the TfL MCP Server to the MCP `2026-07-28` protocol revision and consolidates the project on one Node.js 22 implementation.

### Highlights

- Upgrade production MCP dependencies to the modular TypeScript SDK 2.0 packages.
- Add modern discovery, request metadata, routing headers, deterministic/cacheable list responses, and stateless request handling.
- Keep all six TfL tools and the service-status MCP App available without changing their user-facing behavior.
- Support modern MCP `2026-07-28` over stdio and stateless Streamable HTTP at `/mcp`, with a 2025-era stdio compatibility path for current desktop hosts such as Codex.
- Publish the stdio server as the public `@oneill9/tfl-mcp-server` npm package with a `tfl-mcp-server` CLI.
- Rebuild the OCI image on Node.js 22, with stdio as its default mode and Streamable HTTP enabled by `--http`.
- Add official MCP `2026-07-28` conformance coverage with no expected-failure baseline. The fixture composes the production registrations and shared HTTP adapter with test-only diagnostics required by the referee's hard-coded probes; those diagnostics are not shipped by production.

### Breaking changes and migration

- **Modern HTTP transport:** remote clients must support MCP `2026-07-28` and move from legacy HTTP/SSE to `/mcp`. The stdio distribution additionally accepts 2025-era initialization for desktop-host compatibility.
- **Java retired:** the Java/Gradle/Jetty implementation and ZIP distribution have been removed. Use the MCPB, Node.js source entry point, or Node-based OCI image.
- **HTTP/SSE removed:** remote deployments must replace the legacy SSE endpoint with stateless Streamable HTTP at `/mcp`.
- **Node.js 22 required:** source and MCPB users must use Node.js 22 or later.
- **Artifact names:** the Desktop extension release asset is `tfl-mcp-server.mcpb`, the npm package is `@oneill9/tfl-mcp-server`, and there is no Java ZIP artifact.

For packaged stdio, run `npx -y @oneill9/tfl-mcp-server@2.0.0`. For source-based stdio, build with `cd node && npm ci && npm run build`, then run `node dist/index.js`. For Streamable HTTP, add `--http` and connect to `/mcp`. See [the v2 migration guide](https://github.com/oneill9/tfl-mcp-server/blob/main/docs/migration-v2.md) for complete examples.
