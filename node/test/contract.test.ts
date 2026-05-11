/**
 * Contract tests via stdio transport — the same mechanism LLM clients (e.g.
 * Claude Desktop) use to connect to MCP servers. Spawns the Node server as a
 * real subprocess and communicates over stdin/stdout.
 *
 * TFL_APP_KEY is deliberately excluded from the subprocess environment to
 * verify the server works correctly without authentication.
 */
import { describe, it, expect, beforeAll, afterAll } from "vitest";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

let client: Client;
let transport: StdioClientTransport;

beforeAll(async () => {
  // Strip API keys so we exercise the unauthenticated code path
  const env = { ...process.env } as Record<string, string>;
  delete env.TFL_APP_KEY;
  delete env.TFL_APP_ID;
  // Ensure we hit the real TfL API
  delete env.TFL_BASE_URL;

  transport = new StdioClientTransport({
    command: "node",
    args: [path.resolve(__dirname, "..", "dist", "index.js")],
    env,
    stderr: "pipe",
  });

  client = new Client({ name: "contract-test", version: "1.0.0" });
  await client.connect(transport);
}, 15000);

afterAll(async () => {
  await client?.close();
});

// --- connectivity ---

describe("connectivity", () => {
  it("server reports correct info", async () => {
    const info = client.getServerVersion();
    expect(info?.name).toBe("TfL");
  });

  it("server advertises all 6 tools", async () => {
    const result = await client.listTools();
    const names = result.tools.map((t) => t.name);
    for (const expected of [
      "service_status", "arrivals",
      "journey", "bike_points",
      "crowding", "fares",
    ]) {
      expect(names).toContain(expected);
    }
  });
});

// --- tools (unauthenticated, live TfL API) ---

describe("tools without API key", () => {
  it("service_status", async () => {
    const result = await client.callTool({ name: "service_status", arguments: { modes: "tube" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.toLowerCase()).toContain("central");
  });

  it("bike_points", async () => {
    const result = await client.callTool({ name: "bike_points", arguments: {} });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text).toContain("BikePoints_");
  });

  it("arrivals", async () => {
    const result = await client.callTool({ name: "arrivals", arguments: { stopName: "Oxford Circus Underground Station" } });
    expect(result.isError).toBeFalsy();
  });

  it("journey", async () => {
    const result = await client.callTool({ name: "journey", arguments: { from: "51.4952,-0.1441", to: "51.5179,-0.0816" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.length).toBeGreaterThan(0);
  }, 15000);

  it("crowding", async () => {
    const result = await client.callTool({ name: "crowding", arguments: { stopName: "Oxford Circus Underground Station" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.length).toBeGreaterThan(0);
  }, 15000);

  it("fares", async () => {
    const result = await client.callTool({ name: "fares", arguments: { fromName: "Oxford Circus Underground Station", toName: "Bond Street Underground Station" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.length).toBeGreaterThan(0);
  });
});
