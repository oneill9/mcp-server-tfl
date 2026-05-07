# Installation

There are three ways to run the TfL MCP server: **Node.js MCPB** (recommended), **Docker**, or **Java direct**.

Both the Node.js and Java implementations expose the same 12 tools with identical behaviour — choose whichever fits your environment.

## Claude Desktop Configuration

Open your Claude Desktop configuration file. Usually, this is located at:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

Add the following to your `mcpServers` object, choosing one of the options below:

### Option 1: Node.js MCPB (Recommended)

The lightest option — no Docker or Java required. Just Node.js 20+.

Download `tfl.mcpb` from [GitHub Releases](https://github.com/oneill9/tfl-mcp-server/releases) and install it in Claude Desktop, or add manually:

```json
{
  "mcpServers": {
    "tfl": {
      "command": "npx",
      "args": ["-y", "@oneill9/tfl-mcp-server"],
      "env": {
        "TFL_APP_KEY": "your_api_key_here"
      }
    }
  }
}
```

### Option 2: Docker

You don't need Java or Node.js installed. Docker will download and run the container securely.

```json
{
  "mcpServers": {
    "tfl": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "TFL_APP_KEY=your_api_key_here",
        "ghcr.io/oneill9/tfl-mcp-server:latest"
      ]
    }
  }
}
```

### Option 3: Java Direct

If you prefer to run it using the built distribution ZIP attached to GitHub Releases:

1. Download the latest `tfl-mcp-server.zip` from [GitHub Releases](https://github.com/oneill9/tfl-mcp-server/releases).
2. Unzip it somewhere on your machine.
3. Update your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "tfl": {
      "command": "/path/to/extracted/tfl-mcp-server/bin/tfl-mcp-server",
      "args": [],
      "env": {
        "TFL_APP_KEY": "your_api_key_here"
      }
    }
  }
}
```

## Obtaining the TfL API Key
See the [API Keys](api-keys.md) page for details on how to generate the `TFL_APP_KEY`.
