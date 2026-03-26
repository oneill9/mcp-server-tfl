# Installation

There are two primary ways to run the TfL MCP server: using **Docker**, or running the **latest Java build** locally.

## Claude Desktop Configuration

Open your Claude Desktop configuration file. Usually, this is located at:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

Add the following to your `mcpServers` object, choosing either the Docker approach or the Java approach below:

### Option 1: Docker (Recommended)

You don't need Java installed. Docker will download and run the container securely.

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
        "ghcr.io/oneill9/mcp-server-tfl:latest"
      ]
    }
  }
}
```

### Option 2: Java Direct

If you prefer to run it using the built distribution ZIP attached to GitHub Releases:

1. Download the latest `mcp-server-tfl.zip` from [GitHub Releases](https://github.com/oneill9/mcp-server-tfl/releases).
2. Unzip it somewhere on your machine.
3. Update your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "tfl": {
      "command": "/path/to/extracted/mcp-server-tfl/bin/mcp-server-tfl",
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
