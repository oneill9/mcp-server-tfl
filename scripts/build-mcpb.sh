#!/usr/bin/env bash
set -euo pipefail

NODE_DIR="$(cd "$(dirname "$0")/../node" && pwd)"
OUTPUT_DIR="$(cd "$(dirname "$0")/.." && pwd)/build"

echo "==> Installing dependencies..."
cd "$NODE_DIR"
npm ci --silent

echo "==> Compiling TypeScript..."
npm run build

echo "==> Packing MCPB extension..."
mkdir -p "$OUTPUT_DIR"
mcpb pack "$NODE_DIR" "$OUTPUT_DIR"

echo ""
echo "Done! MCPB bundle written to $OUTPUT_DIR/"
