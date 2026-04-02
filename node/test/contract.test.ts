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

  it("server advertises all 12 tools", async () => {
    const result = await client.listTools();
    const names = result.tools.map((t) => t.name);
    for (const expected of [
      "line_status", "arrivals", "stop_search", "disruptions",
      "journey", "bike_points", "list_modes", "air_quality",
      "road_disruptions", "line_routes", "crowding", "fares",
    ]) {
      expect(names).toContain(expected);
    }
  });
});

// --- tools (unauthenticated, live TfL API) ---

describe("tools without API key", () => {
  it("list_modes", async () => {
    const result = await client.callTool({ name: "list_modes", arguments: {} });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text).toContain("tube");
    expect(text).toContain("bus");
  });

  it("line_status", async () => {
    const result = await client.callTool({ name: "line_status", arguments: { lines: "central" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.toLowerCase()).toContain("central");
  });

  it("stop_search", async () => {
    const result = await client.callTool({ name: "stop_search", arguments: { query: "oxford circus" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.toLowerCase()).toContain("oxford");
  });

  it("air_quality", async () => {
    const result = await client.callTool({ name: "air_quality", arguments: {} });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.length).toBeGreaterThan(0);
  });

  it("bike_points", async () => {
    const result = await client.callTool({ name: "bike_points", arguments: {} });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text).toContain("BikePoints_");
  });

  it("arrivals", async () => {
    // 940GZZLUOXC = Oxford Circus Underground Station
    const result = await client.callTool({ name: "arrivals", arguments: { stopId: "940GZZLUOXC" } });
    expect(result.isError).toBeFalsy();
  });

  it("disruptions", async () => {
    const result = await client.callTool({ name: "disruptions", arguments: { modes: "tube" } });
    expect(result.isError).toBeFalsy();
  });

  it("journey", async () => {
    const result = await client.callTool({ name: "journey", arguments: { from: "51.4952,-0.1441", to: "51.5179,-0.0816" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.length).toBeGreaterThan(0);
  }, 15000);

  it("road_disruptions", async () => {
    const result = await client.callTool({ name: "road_disruptions", arguments: {} });
    expect(result.isError).toBeFalsy();
  });

  it("line_routes", async () => {
    const result = await client.callTool({ name: "line_routes", arguments: { lineId: "central", direction: "outbound" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text).toContain("Central");
  });

  it("crowding", async () => {
    const result = await client.callTool({ name: "crowding", arguments: { naptan: "940GZZLUOXC" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.length).toBeGreaterThan(0);
  });

  it("fares", async () => {
    const result = await client.callTool({ name: "fares", arguments: { fromStopId: "940GZZLUOXC", toStopId: "940GZZLUBND" } });
    expect(result.isError).toBeFalsy();
    const text = (result.content as any)[0].text;
    expect(text.length).toBeGreaterThan(0);
  });
});
