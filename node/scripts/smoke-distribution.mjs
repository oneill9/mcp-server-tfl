#!/usr/bin/env node

import { Client, StreamableHTTPClientTransport } from "@modelcontextprotocol/client";
import { StdioClientTransport } from "@modelcontextprotocol/client/stdio";

const PROTOCOL_VERSION = "2026-07-28";
const SMOKE_TIMEOUT_MS = 15_000;
const EXPECTED_TOOLS = [
  "arrivals",
  "service_status",
  "journey",
  "crowding",
  "fares",
  "bike_points",
];

function fail(message) {
  console.error(message);
  process.exit(1);
}

async function assertModernServer(transport) {
  const client = new Client(
    { name: "distribution-smoke-test", version: "1.0.0" },
    { versionNegotiation: { mode: { pin: PROTOCOL_VERSION } } }
  );

  let timeout;
  const smoke = async () => {
    await client.connect(transport);
    if (client.getProtocolEra() !== "modern") {
      throw new Error(`Expected modern protocol era, got ${client.getProtocolEra()}`);
    }
    if (client.getNegotiatedProtocolVersion() !== PROTOCOL_VERSION) {
      throw new Error(
        `Expected protocol ${PROTOCOL_VERSION}, got ${client.getNegotiatedProtocolVersion()}`
      );
    }

    const tools = await client.listTools(undefined, { cacheMode: "refresh" });
    const toolNames = tools.tools.map(({ name }) => name);
    if (JSON.stringify(toolNames) !== JSON.stringify(EXPECTED_TOOLS)) {
      throw new Error(`Unexpected tools: ${JSON.stringify(toolNames)}`);
    }
  };

  try {
    await Promise.race([
      smoke(),
      new Promise((_, reject) => {
        timeout = setTimeout(() => {
          void transport.close().catch(() => undefined);
          reject(new Error(`Distribution smoke test timed out after ${SMOKE_TIMEOUT_MS}ms`));
        }, SMOKE_TIMEOUT_MS);
      }),
    ]);
  } finally {
    clearTimeout(timeout);
    await client.close();
  }
}

const [mode, ...args] = process.argv.slice(2);

if (mode === "stdio") {
  const [command, ...commandArgs] = args;
  if (!command) fail("Usage: smoke-distribution.mjs stdio <command> [args...]");
  await assertModernServer(
    new StdioClientTransport({
      command,
      args: commandArgs,
      env: process.env,
      stderr: "inherit",
    })
  );
} else if (mode === "http") {
  const [endpoint] = args;
  if (!endpoint) fail("Usage: smoke-distribution.mjs http <endpoint>");
  await assertModernServer(new StreamableHTTPClientTransport(new URL(endpoint)));
} else {
  fail("Usage: smoke-distribution.mjs <stdio|http> ...");
}

console.log(`${mode} distribution negotiated MCP ${PROTOCOL_VERSION} and listed all six tools`);
