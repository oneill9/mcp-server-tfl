#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NODE_DIR="$REPO_ROOT/node"
SMOKE_CLIENT="$NODE_DIR/scripts/smoke-distribution.mjs"
SMOKE_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/tfl-npm-smoke.XXXXXX")"
PACK_DIR="$SMOKE_ROOT/pack"
INSTALL_DIR="$SMOKE_ROOT/install"

cleanup() {
  local exit_code=$?
  if [[ -n "${SMOKE_ROOT:-}" && -d "$SMOKE_ROOT" && "$(basename "$SMOKE_ROOT")" == tfl-npm-smoke.* ]]; then
    rm -rf -- "$SMOKE_ROOT"
  fi
  return "$exit_code"
}
trap cleanup EXIT

mkdir -p "$PACK_DIR" "$INSTALL_DIR"

(
  cd "$NODE_DIR"
  NPM_CONFIG_CACHE="$SMOKE_ROOT/npm-cache" npm pack --pack-destination "$PACK_DIR"
)
tarball_count="$(find "$PACK_DIR" -maxdepth 1 -type f -name '*.tgz' -print | wc -l | tr -d ' ')"
if [[ "$tarball_count" -ne 1 ]]; then
  echo "npm pack must create exactly one tarball; found $tarball_count" >&2
  exit 1
fi
tarball_path="$(find "$PACK_DIR" -maxdepth 1 -type f -name '*.tgz' -print -quit)"

package_manifest="$(tar -xOf "$tarball_path" package/package.json)"
node --input-type=module - "$package_manifest" <<'NODE'
const manifest = JSON.parse(process.argv[2]);
if (manifest.bin?.["tfl-mcp-server"] !== "dist/index.js") {
  throw new Error("npm package must expose the tfl-mcp-server CLI from dist/index.js");
}
if (manifest.publishConfig?.access !== "public") {
  throw new Error("scoped npm package must publish with public access");
}
NODE

archive_entries="$(tar -tf "$tarball_path")"
if ! grep -qx 'package/dist/index.js' <<<"$archive_entries"; then
  echo "npm package is missing dist/index.js" >&2
  exit 1
fi
if [[ "$(tar -xOf "$tarball_path" package/dist/index.js | sed -n '1p')" != '#!/usr/bin/env node' ]]; then
  echo "Packed npm CLI must begin with a Node.js shebang" >&2
  exit 1
fi
if grep -Eq '^package/(src|test)/' <<<"$archive_entries"; then
  echo "npm package must not contain TypeScript sources or tests" >&2
  exit 1
fi

NPM_CONFIG_CACHE="$SMOKE_ROOT/npm-cache" npm install --silent --ignore-scripts --prefix "$INSTALL_DIR" "$tarball_path"
installed_cli="$INSTALL_DIR/node_modules/.bin/tfl-mcp-server"
if [[ ! -x "$installed_cli" ]]; then
  echo "Installed npm package does not expose an executable tfl-mcp-server CLI" >&2
  exit 1
fi

node "$SMOKE_CLIENT" stdio "$installed_cli"
echo "npm tarball installed cleanly and exposed a modern stdio server"
