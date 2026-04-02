import { describe, it, expect, beforeAll, afterAll } from "vitest";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// --- Stub TfL API ---

const STUBS: Record<string, { status: number; body: any }> = {
  "/Line/central/Status": {
    status: 200,
    body: [{ id: "central", name: "Central", lineStatuses: [{ statusSeverityDescription: "Good Service", reason: "" }] }],
  },
  "/Line/central,victoria/Status": {
    status: 200,
    body: [
      { id: "central", name: "Central", lineStatuses: [{ statusSeverityDescription: "Good Service", reason: "" }] },
      { id: "victoria", name: "Victoria", lineStatuses: [{ statusSeverityDescription: "Minor Delays", reason: "Earlier signal failure" }] },
    ],
  },
  "/StopPoint/940GZZLUOXC/Arrivals": {
    status: 200,
    body: [
      { lineName: "Central", platformName: "Eastbound - Platform 2", destinationName: "Epping", timeToStation: 120 },
      { lineName: "Central", platformName: "Eastbound - Platform 2", destinationName: "Epping", timeToStation: 300 },
    ],
  },
  "/StopPoint/Search/oxford": {
    status: 200,
    body: {
      matches: [
        { id: "940GZZLUOXC", name: "Oxford Circus Underground Station", lat: 51.515, lon: -0.1416 },
        { id: "490000173RC", name: "Oxford Circus", lat: 51.5148, lon: -0.1418 },
      ],
    },
  },
  "/Line/Mode/tube/Disruption": {
    status: 200,
    body: [
      { lineId: "central", description: "Minor delays due to earlier signal failure near Oxford Circus" },
      { lineId: "jubilee", description: "Good service" },
    ],
  },
  "/Line/Meta/Modes": {
    status: 200,
    body: [{ modeName: "tube", isTflService: true }, { modeName: "bus", isTflService: true }, { modeName: "dlr", isTflService: true }],
  },
  "/Line/central/Route/Sequence/outbound": {
    status: 200,
    body: {
      lineId: "central", lineName: "Central", direction: "outbound",
      stopPointSequences: [{
        branchId: 0,
        stopPoint: [
          { id: "940GZZLUEPG", name: "Epping Underground Station", lat: 51.6937, lon: 0.1139 },
          { id: "940GZZLUTHB", name: "Theydon Bois Underground Station", lat: 51.6717, lon: 0.1033 },
          { id: "940GZZLUOXC", name: "Oxford Circus Underground Station", lat: 51.515, lon: -0.1416 },
        ],
      }],
    },
  },
  "/Crowding/940GZZLUOXC/Live": {
    status: 200,
    body: { dataAvailable: true, percentageOfBaseline: 0.6863 },
  },
  "/StopPoint/940GZZLUOXC/FareTo/940GZZLUBND": {
    status: 200,
    body: [{
      header: "Single Fare Finder",
      rows: [{
        from: "Oxford Circus", to: "Bond Street", passengerType: "Adult",
        ticketsAvailable: [
          { ticketType: "Pay as you go", ticketTime: "Peak", cost: "2.80", mode: "tube" },
          { ticketType: "Pay as you go", ticketTime: "Off Peak", cost: "2.70", mode: "tube" },
          { ticketType: "CashSingle", ticketTime: "Anytime", cost: "6.70", mode: "tube" },
        ],
      }],
    }],
  },
  "/Journey/JourneyResults/1000123/to/1000456": {
    status: 200,
    body: {
      journeys: [
        { duration: 25, legs: [{ summary: "Take Central line to Bank", duration: 10, instruction: { summary: "Take Central line to Bank" } }, { summary: "Walk to destination", duration: 5, instruction: { summary: "Walk to destination" } }] },
        { duration: 35, legs: [{ summary: "Take bus 23 to Liverpool Street", duration: 20, instruction: { summary: "Take bus 23 to Liverpool Street" } }] },
      ],
    },
  },
  "/BikePoint": {
    status: 200,
    body: [
      {
        id: "BikePoints_1", commonName: "River Street, Clerkenwell",
        lat: 51.5292, lon: -0.1097,
        additionalProperties: [
          { key: "NbBikes", value: "15" },
          { key: "NbEmptyDocks", value: "8" },
        ],
      },
      {
        id: "BikePoints_2", commonName: "Phillimore Gardens, Kensington",
        lat: 51.4991, lon: -0.1984,
        additionalProperties: [
          { key: "NbBikes", value: "3" },
          { key: "NbEmptyDocks", value: "14" },
        ],
      },
    ],
  },
  "/BikePoint/Search/clerkenwell": {
    status: 200,
    body: [
      {
        id: "BikePoints_1", commonName: "River Street, Clerkenwell",
        lat: 51.5292, lon: -0.1097,
        additionalProperties: [
          { key: "NbBikes", value: "15" },
          { key: "NbEmptyDocks", value: "8" },
        ],
      },
    ],
  },
  // error stubs
  "/Line/bad-line/Status": { status: 500, body: "" },
  "/StopPoint/notfound/Arrivals": { status: 404, body: "" },
  "/Line/Mode/unknown-mode/Disruption": { status: 400, body: "" },
};

let mockServer: http.Server;
let mockPort: number;
let client: Client;
let transport: StdioClientTransport;

beforeAll(async () => {
  // Start mock TfL API
  mockServer = http.createServer((req, res) => {
    const url = new URL(req.url!, `http://localhost`);
    const stub = STUBS[url.pathname];
    if (stub) {
      res.writeHead(stub.status, { "Content-Type": "application/json" });
      res.end(JSON.stringify(stub.body));
    } else {
      res.writeHead(404);
      res.end("Not Found");
    }
  });

  await new Promise<void>((resolve) => {
    mockServer.listen(0, () => resolve());
  });
  mockPort = (mockServer.address() as any).port;

  // Start MCP server via stdio
  transport = new StdioClientTransport({
    command: "node",
    args: [path.resolve(__dirname, "..", "dist", "index.js")],
    env: {
      ...process.env as Record<string, string>,
      TFL_BASE_URL: `http://localhost:${mockPort}`,
    },
    stderr: "pipe",
  });

  client = new Client({ name: "test-client", version: "1.0.0" });
  await client.connect(transport);
}, 15000);

afterAll(async () => {
  await client?.close();
  await new Promise<void>((resolve) => {
    if (mockServer) mockServer.close(() => resolve());
    else resolve();
  });
});

// --- tool list ---

describe("tool listing", () => {
  it("lists all 10 tools", async () => {
    const result = await client.listTools();
    const names = result.tools.map((t) => t.name);
    for (const expected of [
      "line_status", "arrivals", "stop_search", "disruptions",
      "journey", "list_modes", "line_routes", "crowding", "fares", "bike_points",
    ]) {
      expect(names).toContain(expected);
    }
  });
});

// --- tool annotations ---

describe("tool annotations", () => {
  it("all tools have readOnlyHint, destructiveHint, and title", async () => {
    const result = await client.listTools();
    const expected = new Set([
      "line_status", "arrivals", "stop_search", "disruptions",
      "journey", "list_modes", "line_routes", "crowding", "fares", "bike_points",
    ]);
    for (const tool of result.tools) {
      if (!expected.has(tool.name)) continue;
      expect(tool.annotations?.title, `${tool.name} should have a title`).toBeTruthy();
      expect(tool.annotations?.readOnlyHint, `${tool.name} should have readOnlyHint=true`).toBe(true);
      expect(tool.annotations?.destructiveHint, `${tool.name} should have destructiveHint=false`).toBe(false);
    }
  });
});

// --- line_status ---

describe("line_status", () => {
  it("returns status for central line", async () => {
    const result = await client.callTool({ name: "line_status", arguments: { lines: "central" } });
    const text = (result.content as any)[0].text;
    expect(text.toLowerCase()).toContain("central");
    expect(text).toContain("Good Service");
  });

  it("accepts multiple lines", async () => {
    const result = await client.callTool({ name: "line_status", arguments: { lines: "central,victoria" } });
    const text = (result.content as any)[0].text;
    expect(text.toLowerCase()).toContain("central");
    expect(text.toLowerCase()).toContain("victoria");
  });

  it("returns error on 500", async () => {
    const result = await client.callTool({ name: "line_status", arguments: { lines: "bad-line" } });
    expect(result.isError).toBe(true);
    const text = (result.content as any)[0].text;
    expect(text).toContain("500");
  });
});

// --- arrivals ---

describe("arrivals", () => {
  it("returns live arrivals", async () => {
    const result = await client.callTool({ name: "arrivals", arguments: { stopId: "940GZZLUOXC" } });
    const text = (result.content as any)[0].text;
    expect(text).toContain("Central");
    expect(text).toContain("Epping");
    expect(text).toContain("2 min");
  });

  it("returns error on 404", async () => {
    const result = await client.callTool({ name: "arrivals", arguments: { stopId: "notfound" } });
    expect(result.isError).toBe(true);
    const text = (result.content as any)[0].text;
    expect(text).toContain("404");
  });
});

// --- stop_search ---

describe("stop_search", () => {
  it("returns matching stops", async () => {
    const result = await client.callTool({ name: "stop_search", arguments: { query: "oxford" } });
    const text = (result.content as any)[0].text;
    expect(text).toContain("Oxford Circus");
    expect(text).toContain("940GZZLUOXC");
  });
});

// --- disruptions ---

describe("disruptions", () => {
  it("returns disruptions by mode", async () => {
    const result = await client.callTool({ name: "disruptions", arguments: { modes: "tube" } });
    const text = (result.content as any)[0].text;
    expect(text).toContain("central");
    expect(text).toContain("signal failure");
  });

  it("returns error on 400", async () => {
    const result = await client.callTool({ name: "disruptions", arguments: { modes: "unknown-mode" } });
    expect(result.isError).toBe(true);
    const text = (result.content as any)[0].text;
    expect(text).toContain("400");
  });
});

// --- journey ---

describe("journey", () => {
  it("returns plan between two points", async () => {
    const result = await client.callTool({ name: "journey", arguments: { from: "1000123", to: "1000456" } });
    const text = (result.content as any)[0].text;
    expect(text).toContain("25");
    expect(text).toContain("Bank");
  });
});

// --- list_modes ---

describe("list_modes", () => {
  it("returns available modes", async () => {
    const result = await client.callTool({ name: "list_modes", arguments: {} });
    const text = (result.content as any)[0].text;
    expect(text).toContain("tube");
    expect(text).toContain("bus");
    expect(text).toContain("dlr");
  });
});

// --- line_routes ---

describe("line_routes", () => {
  it("returns stops in order", async () => {
    const result = await client.callTool({ name: "line_routes", arguments: { lineId: "central", direction: "outbound" } });
    const text = (result.content as any)[0].text;
    expect(text).toContain("Central");
    expect(text).toContain("Epping");
    expect(text).toContain("Oxford Circus");
  });
});

// --- crowding ---

describe("crowding", () => {
  it("returns live data", async () => {
    const result = await client.callTool({ name: "crowding", arguments: { naptan: "940GZZLUOXC" } });
    const text = (result.content as any)[0].text;
    expect(text).toMatch(/0\.6863|69/);
  });
});

// --- bike_points ---

describe("bike_points", () => {
  it("returns all stations when no query given", async () => {
    const result = await client.callTool({ name: "bike_points", arguments: {} });
    const text = (result.content as any)[0].text;
    expect(text).toContain("River Street");
    expect(text).toContain("bikes");
    expect(text).toContain("docks");
  });

  it("filters results when query is provided", async () => {
    const result = await client.callTool({ name: "bike_points", arguments: { query: "clerkenwell" } });
    const text = (result.content as any)[0].text;
    expect(text).toContain("Clerkenwell");
  });
});

// --- fares ---

describe("fares", () => {
  it("returns fare between stops", async () => {
    const result = await client.callTool({ name: "fares", arguments: { fromStopId: "940GZZLUOXC", toStopId: "940GZZLUBND" } });
    const text = (result.content as any)[0].text;
    expect(text).toContain("Oxford Circus");
    expect(text).toContain("Bond Street");
    expect(text).toContain("2.80");
  });
});
