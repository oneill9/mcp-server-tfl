#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MCPB_PATH="$REPO_ROOT/build/tfl-mcp-server.mcpb"
IMAGE_NAME="${TFL_SMOKE_IMAGE:-tfl-mcp-server:smoke}"
SMOKE_CLIENT="$REPO_ROOT/node/scripts/smoke-distribution.mjs"

assert_distribution_sources() {
  grep -q '^FROM node:22-alpine' "$REPO_ROOT/Dockerfile"
  if grep -Eq 'eclipse-temurin|gradle|java' "$REPO_ROOT/Dockerfile"; then
    echo "Dockerfile must contain only the Node.js implementation" >&2
    return 1
  fi

  node --input-type=module - "$REPO_ROOT/node/manifest.json" "$REPO_ROOT/server.json" <<'NODE'
import { readFileSync } from "node:fs";

const [manifestPath, serverPath] = process.argv.slice(2);
const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));
const registry = JSON.parse(readFileSync(serverPath, "utf8"));

if (manifest.manifest_version !== "0.4") {
  throw new Error("MCPB manifest must use version 0.4");
}
if (manifest.server?.type !== "node" || manifest.server?.entry_point !== "dist/index.js") {
  throw new Error("MCPB must launch the Node distribution entry point");
}
if (manifest.compatibility?.runtimes?.node !== ">=22.0.0") {
  throw new Error("MCPB must declare its Node.js 22 runtime requirement");
}
if (registry.packages?.[0]?.registryType !== "oci") {
  throw new Error("Registry package must remain OCI-based");
}
if (registry.packages?.[0]?.transport?.type !== "stdio") {
  throw new Error("Registry OCI transport must remain stdio");
}

const manifestEnvironment = manifest.server?.mcp_config?.env ?? {};
const registryEnvironment = registry.packages?.[0]?.environmentVariables ?? [];
for (const name of ["TFL_APP_ID", "TFL_APP_KEY"]) {
  if (!(name in manifestEnvironment)) {
    throw new Error(`${name} must be passed to the MCPB through the environment`);
  }
  const registryVariable = registryEnvironment.find((variable) => variable.name === name);
  if (!registryVariable?.isSecret) {
    throw new Error(`${name} must be declared as a registry secret environment variable`);
  }
}
NODE
}

smoke_mcpb() {
  bash "$REPO_ROOT/scripts/build-mcpb.sh"

  local unpack_dir
  unpack_dir="$(mktemp -d)"
  unpack_dir="$(cd "$unpack_dir" && pwd -P)"
  trap 'rm -rf "$unpack_dir"' RETURN
  unzip -q "$MCPB_PATH" -d "$unpack_dir"
  mcpb validate "$unpack_dir/manifest.json"
  node "$SMOKE_CLIENT" stdio node "$unpack_dir/dist/index.js"
  rm -rf "$unpack_dir"
  trap - RETURN
}

container_port() {
  local container_id="$1"
  local mapping
  for _ in {1..30}; do
    mapping="$(docker port "$container_id" 8080/tcp 2>/dev/null || true)"
    if [[ "$mapping" =~ :([0-9]+)$ ]]; then
      printf '%s\n' "${BASH_REMATCH[1]}"
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_for_http_container() {
  local container_id="$1"
  local container_logs
  for _ in {1..30}; do
    container_logs="$(docker logs "$container_id" 2>&1 || true)"
    if [[ "$container_logs" == *"listening on http://"* ]]; then
      return 0
    fi
    if [[ "$(docker inspect --format '{{.State.Running}}' "$container_id" 2>/dev/null || true)" != "true" ]]; then
      echo "$container_logs" >&2
      return 1
    fi
    sleep 1
  done
  echo "$container_logs" >&2
  return 1
}

smoke_containers() {
  docker build --tag "$IMAGE_NAME" "$REPO_ROOT"

  local image_environment
  image_environment="$(docker inspect --format '{{json .Config.Env}}' "$IMAGE_NAME")"
  if [[ "$image_environment" == *"TFL_APP_ID="* || "$image_environment" == *"TFL_APP_KEY="* ]]; then
    echo "TfL credentials must not be baked into the container image" >&2
    return 1
  fi

  node "$SMOKE_CLIENT" stdio docker run --rm -i "$IMAGE_NAME"

  local container_id
  container_id="$(docker run --detach --rm --publish 127.0.0.1::8080 "$IMAGE_NAME" --http)"
  if [[ ! "$container_id" =~ ^[0-9a-f]{12,64}$ ]]; then
    echo "Docker returned an unexpected container id: $container_id" >&2
    return 1
  fi

  cleanup_container() {
    if docker inspect "$container_id" >/dev/null 2>&1; then
      docker stop "$container_id" >/dev/null
    fi
  }
  trap cleanup_container RETURN

  local port
  port="$(container_port "$container_id")"
  wait_for_http_container "$container_id"
  node "$SMOKE_CLIENT" http "http://127.0.0.1:$port/mcp"

  cleanup_container
  trap - RETURN
}

assert_distribution_sources
if [[ "${SKIP_NPM_SMOKE:-0}" == "1" ]]; then
  echo "Skipping npm package smoke test because SKIP_NPM_SMOKE=1"
else
  bash "$REPO_ROOT/scripts/test-npm-distribution.sh"
fi

if [[ "${SKIP_MCPB_SMOKE:-0}" == "1" ]]; then
  echo "Skipping MCPB smoke test because SKIP_MCPB_SMOKE=1"
else
  smoke_mcpb
fi

if [[ "${SKIP_DOCKER_SMOKE:-0}" == "1" ]]; then
  echo "Skipping container smoke tests because SKIP_DOCKER_SMOKE=1"
elif docker info >/dev/null 2>&1; then
  smoke_containers
else
  echo "Skipping container smoke tests because the Docker daemon is unavailable"
fi
