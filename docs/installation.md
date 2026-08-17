# Installation

The TfL MCP server is distributed as a **Node.js MCPB** (recommended for Claude Desktop), a public npm package, and a Node.js 22 OCI image. You can also build and run it directly from source.

Version 2 uses MCP `2026-07-28`. Streamable HTTP is modern-only; stdio also accepts 2025-era initialization so current desktop hosts such as Codex can load the same tools and MCP App resource. Legacy HTTP/SSE remains unsupported.

## Claude Desktop Configuration

Open your Claude Desktop configuration file. Usually, this is located at:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

Add the following to your `mcpServers` object, choosing one of the options below:

### Option 1: Node.js MCPB (Recommended)

The lightest option. The extension declares its Node.js 22 runtime requirement.

Download `tfl-mcp-server.mcpb` from [GitHub Releases](https://github.com/oneill9/tfl-mcp-server/releases) and install it in Claude Desktop. Claude Desktop prompts for the optional TfL credentials declared by the bundle.

### Option 2: npm

Install Node.js 22 or later, then configure the published package as a stdio server:

```json
{
  "mcpServers": {
    "tfl-mcp-server": {
      "command": "npx",
      "args": ["-y", "@oneill9/tfl-mcp-server@2.0.0"],
      "env": {
        "TFL_APP_KEY": "your_api_key_here"
      }
    }
  }
}
```

The explicit version keeps installations reproducible. Update it when adopting a later release.

### Option 3: Docker

You don't need Node.js installed. Docker starts the server over stdio by default.

```json
{
  "mcpServers": {
    "tfl-mcp-server": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "TFL_APP_KEY",
        "ghcr.io/oneill9/tfl-mcp-server:latest"
      ],
      "env": {
        "TFL_APP_KEY": "your_api_key_here"
      }
    }
  }
}
```

### Option 4: Run from Source

Install Node.js 22 or later, clone the repository, then run:

```sh
cd node
npm ci
npm run build
```

Point your client at the built stdio entry point:

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

## Streamable HTTP

The source and container distributions can expose stateless Streamable HTTP at `/mcp`:

```sh
# Source checkout
cd node
HOST=127.0.0.1 PORT=8080 node dist/index.js --http

# OCI image
docker run --rm -p 8080:8080 ghcr.io/oneill9/tfl-mcp-server:2.0.0 --http
```

Connect an MCP `2026-07-28` client to `http://127.0.0.1:8080/mcp`. The default hostname allowlist is `localhost`, `127.0.0.1`, and `[::1]`; set `MCP_ALLOWED_HOSTS` to a comma-separated list for other hostnames.

The endpoint accepts JSON request bodies up to 1 MiB by default and uses 30-second request and 10-second header timeouts. Override these positive integer values with `MCP_MAX_REQUEST_BODY_BYTES`, `MCP_REQUEST_TIMEOUT_MS`, and `MCP_HEADERS_TIMEOUT_MS`; the header timeout must not exceed the request timeout.

The HTTP endpoint has no OAuth or application-level authentication. Put it behind a trusted access layer before exposing it beyond the local machine.

## Obtaining the TfL API Key
See the [API Keys](api-keys.md) page for details on how to generate the `TFL_APP_KEY`.
