#!/usr/bin/env bash

# Runs the official frozen 2026-07-28 server requirement set with no expected-
# failure baseline. The fixture composes the real production TfL registrations
# with test-only diagnostics required by the referee's hard-coded probes, then
# serves both through the same modern-only HTTP adapter as production. The
# diagnostic registrations are never exposed by the production entry point.
set -euo pipefail

conformance_port="${MCP_CONFORMANCE_PORT:-3100}"
conformance_url="http://127.0.0.1:${conformance_port}/mcp"
results_dir="${MCP_CONFORMANCE_RESULTS_DIR:-/tmp/tfl-mcp-conformance-results}"

PORT="${conformance_port}" node --import tsx test/support/run-conformance-server.ts &
server_pid=$!

cleanup() {
  kill "${server_pid}" 2>/dev/null || true
  wait "${server_pid}" 2>/dev/null || true
}
trap cleanup EXIT

for _attempt in {1..30}; do
  if curl --silent --max-time 2 "${conformance_url}" >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "${server_pid}" 2>/dev/null; then
    echo "Conformance fixture exited before becoming ready" >&2
    exit 1
  fi
  sleep 0.2
done

if ! kill -0 "${server_pid}" 2>/dev/null; then
  echo "Conformance fixture is not running" >&2
  exit 1
fi

npx @modelcontextprotocol/conformance server \
  --url "${conformance_url}" \
  --requirements 2026-07-28 \
  -o "${results_dir}"
