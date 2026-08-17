# Migrating to v2.0.0

Version 2 is a breaking protocol and runtime migration. The six TfL tools and service-status MCP App remain available, but clients and deployments must use the new transport stack.

## Requirements

- Use an MCP client that supports `2026-07-28` for Streamable HTTP. The stdio entry point also supports 2025-era initialization for desktop-host compatibility.
- Use Node.js 22 or later for source and MCPB installations.
- Remove any Java, Gradle, Jetty, ZIP-distribution, or legacy HTTP/SSE configuration.

Version 2's `/mcp` HTTP endpoint intentionally rejects legacy requests. Its stdio entry point serves both modern `2026-07-28` clients and 2025-era desktop clients from the same tool registrations; the retired HTTP/SSE transport does not return.

## Local and Desktop clients

The recommended Claude Desktop path is to replace the old installation with `tfl-mcp-server.mcpb` from the v2 GitHub release.

The same stdio server is also published to npm. A client that launches packages can use:

```json
{
  "mcpServers": {
    "tfl-mcp-server": {
      "command": "npx",
      "args": ["-y", "@oneill9/tfl-mcp-server@2.0.0"]
    }
  }
}
```

For a source checkout, replace a Java launcher or older Node entry point with the built v2 stdio entry point:

```sh
cd node
npm ci
npm run build
node dist/index.js
```

An equivalent client configuration is:

```json
{
  "mcpServers": {
    "tfl-mcp-server": {
      "command": "node",
      "args": ["/absolute/path/to/tfl-mcp-server/node/dist/index.js"],
      "env": {
        "TFL_APP_KEY": "your_api_key_here"
      }
    }
  }
}
```

## Containers

The OCI image now contains the Node.js implementation. Existing stdio container configuration can keep the same image name:

```sh
docker run --rm -i -e TFL_APP_KEY ghcr.io/oneill9/tfl-mcp-server:2.0.0
```

The former HTTP/SSE transport is not available. Start stateless Streamable HTTP instead:

```sh
docker run --rm -p 8080:8080 -e TFL_APP_KEY ghcr.io/oneill9/tfl-mcp-server:2.0.0 --http
```

Connect to `http://127.0.0.1:8080/mcp`. When using a hostname other than the local defaults, pass a comma-separated allowlist through `MCP_ALLOWED_HOSTS`.

The HTTP endpoint does not provide OAuth or application-level authentication. Put non-local deployments behind a trusted access layer.

## Verification

From a clean source checkout:

```sh
cd node
npm ci
npm run build
npm test
npm run test:conformance
npm run contractTest
```

The conformance command composes the production TfL registrations with the shared modern-only HTTP adapter. Test-only diagnostic registrations satisfy the referee's hard-coded probes and are never part of the production server.

The contract tests call the live TfL API. A `TFL_APP_KEY` is optional but recommended to avoid anonymous rate limits. From the repository root, `bash scripts/test-distributions.sh` additionally packs and clean-installs the npm CLI, then builds and tests the MCPB and OCI distributions; container tests are skipped when Docker is unavailable.
