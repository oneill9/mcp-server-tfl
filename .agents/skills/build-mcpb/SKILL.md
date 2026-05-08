---
name: build-mcpb
description: Build the MCPB Desktop extension binary for testing with Claude Desktop.
---

Build the MCPB extension bundle for the Node.js TfL MCP server.

## Steps

1. **Run the build script** — execute `bash scripts/build-mcpb.sh` from the project root. This will:
   - Install Node.js dependencies (`npm ci`)
   - Compile TypeScript (`npm run build`)
   - Pack the extension into `build/tfl.mcpb` using `mcpb pack`

2. **Report the result** — tell the user the path to the built `.mcpb` file and remind them they can install it with `mcpb install build/tfl.mcpb` or double-click it to open in Claude Desktop.
