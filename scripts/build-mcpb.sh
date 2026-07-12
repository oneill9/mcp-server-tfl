#!/usr/bin/env bash
set -euo pipefail

NODE_DIR="$(cd "$(dirname "$0")/../node" && pwd)"
OUTPUT_DIR="$(cd "$(dirname "$0")/.." && pwd)/build"

echo "==> Installing dependencies..."
cd "$NODE_DIR"
npm ci --silent

echo "==> Compiling TypeScript..."
npm run build

echo "==> Pruning dev dependencies..."
npm prune --production

restore_dev_dependencies() {
  echo "==> Restoring dev dependencies..."
  npm ci --silent
}

trap restore_dev_dependencies EXIT

echo "==> Packing MCPB extension..."
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR/tfl-mcp-server.mcpb"
mcpb pack "$NODE_DIR" "$OUTPUT_DIR/tfl-mcp-server.mcpb"

trap - EXIT
restore_dev_dependencies

echo ""
echo "Done! MCPB bundle written to $OUTPUT_DIR/tfl-mcp-server.mcpb"
echo "To test locally, you can double-click the .mcpb file to install it in Claude Desktop."
