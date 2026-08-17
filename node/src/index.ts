#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/server";
import { serveStdio } from "@modelcontextprotocol/server/stdio";
import { realpathSync } from "node:fs";
import { pathToFileURL } from "node:url";
import { z } from "zod";
import { renderServiceStatusHtml, type LineStatusData } from "./ui.js";

const RESOURCE_MIME_TYPE = "text/html;profile=mcp-app";

const TFL_BASE = process.env.TFL_BASE_URL ?? "https://api.tfl.gov.uk";

function withAuth(url: string): string {
  const params: string[] = [];
  if (process.env.TFL_APP_ID) params.push(`app_id=${process.env.TFL_APP_ID}`);
  if (process.env.TFL_APP_KEY) params.push(`app_key=${process.env.TFL_APP_KEY}`);
  if (params.length === 0) return url;
  return `${url}?${params.join("&")}`;
}

function encodePath(value: string): string {
  return encodeURIComponent(value);
}

function encodeSegments(csv: string): string {
  return csv
    .split(",")
    .map((s) => encodeURIComponent(s.trim()))
    .join(",");
}

async function httpGet(path: string): Promise<any> {
  const url = withAuth(`${TFL_BASE}${path}`);
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`TfL API returned HTTP ${response.status}`);
  }
  return response.json();
}

const ANNOTATIONS = {
  readOnlyHint: true as const,
  destructiveHint: false as const,
  idempotentHint: true as const,
  openWorldHint: true as const,
};

const SERVICE_STATUS_MODES = ["tube"] as const;
const SERVICE_STATUS_MODES_DESCRIPTION =
  "TfL mode to query. Currently only tube is supported.";
const SERVICE_STATUS_LOG_SCHEMA = {
  type: "object",
  properties: {
    modes: {
      type: "string",
      description: SERVICE_STATUS_MODES_DESCRIPTION,
      enum: SERVICE_STATUS_MODES,
    },
  },
  required: ["modes"],
  additionalProperties: false,
};
type ServiceStatusModes = (typeof SERVICE_STATUS_MODES)[number];
type ServiceStatusArgs = { modes: ServiceStatusModes };
const SERVICE_STATUS_INPUT_SCHEMA = z
  .object({
    modes: z.enum(SERVICE_STATUS_MODES).describe(SERVICE_STATUS_MODES_DESCRIPTION),
  })
  .strict();

export function registerTflTools(server: McpServer): void {

function logToolCall(
  toolName: string,
  path: string | null,
  rawArgs: unknown,
  inputSchema: unknown,
  validationError?: string
): void {
  console.error(
    "Inbound MCP tool call:",
    JSON.stringify({
      tool: toolName,
      path: path ?? "<not resolved>",
      rawArgs,
      inputSchema,
      validationError: validationError ?? null,
    })
  );
}

async function resolveStopName(query: string): Promise<string> {
  const data = await httpGet(`/StopPoint/Search/${encodePath(query)}`);
  if (!data.matches || data.matches.length === 0) {
    throw new Error(`No stop found for name: ${query}`);
  }
  return data.matches[0].id;
}

// --- arrivals ---
server.registerTool(
  "arrivals",
  {
    description: "Get live arrivals at a TfL stop.",
    inputSchema: z.object({ stopName: z.string().describe("Stop name or search term, e.g. oxford") }),
    annotations: { ...ANNOTATIONS, title: "Live Arrivals" },
  },
  async ({ stopName }) => {
    try {
      const stopId = await resolveStopName(stopName);
      const data: any[] = await httpGet(`/StopPoint/${encodePath(stopId)}/Arrivals`);
      data.sort((a, b) => a.timeToStation - b.timeToStation);
      const lines = data.map((a) => {
        const minutes = Math.floor(a.timeToStation / 60);
        let line = `${a.lineName} → ${a.destinationName}: ${minutes} min`;
        if (a.platformName) line += ` (${a.platformName})`;
        return line;
      });
      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching arrivals: ${e.message}` }], isError: true };
    }
  }
);

// --- service_status UI resource ---

const SERVICE_STATUS_UI_URI = "ui://tfl/service-status";

server.registerResource(
  "Service Status Board",
  SERVICE_STATUS_UI_URI,
  {
    description: "Live London transport service status board",
    mimeType: RESOURCE_MIME_TYPE,
  },
  async () => ({
    contents: [
      {
        uri: SERVICE_STATUS_UI_URI,
        mimeType: RESOURCE_MIME_TYPE,
        text: renderServiceStatusHtml(),
      },
    ],
  })
);

// --- service_status ---

/** Parse raw TfL API response into a structured view model. */
function parseLineStatuses(data: any[]): LineStatusData[] {
  return data.map((line) => ({
    id: line.id ?? "",
    name: line.name ?? "",
    statuses: (line.lineStatuses ?? []).map((s: any) => ({
      severity: s.statusSeverityDescription ?? "",
      reason: s.reason || undefined,
    })),
  }));
}

/** Format structured line statuses into human-readable text. */
function formatLineStatuses(lines: LineStatusData[]): string {
  return lines
    .map((line) => {
      const parts = line.statuses.map((s) =>
        s.reason ? `${s.severity} — ${s.reason}` : s.severity
      );
      return `${line.name}: ${parts.join("; ")}`;
    })
    .join("\n");
}

server.registerTool(
  "service_status",
  {
    title: "Service Status",
    description: "Get the current operational status and delays for one or more TfL public transport modes.",
    inputSchema: SERVICE_STATUS_INPUT_SCHEMA,
    annotations: { ...ANNOTATIONS, title: "Service Status" },
    _meta: {
      ui: { resourceUri: SERVICE_STATUS_UI_URI },
      "ui/resourceUri": SERVICE_STATUS_UI_URI,
    },
  },
  async (args: ServiceStatusArgs) => {
    const { modes } = args;
    const path = `/Line/Mode/${encodeSegments(modes)}/Status`;
    logToolCall("service_status", path, { modes }, SERVICE_STATUS_LOG_SCHEMA);
    try {
      const data: any[] = await httpGet(path);
      const structured = parseLineStatuses(data);
      return {
        structuredContent: structured,
        content: [
          { type: "text" as const, text: formatLineStatuses(structured) },
          {
            type: "resource" as const,
            resource: {
              uri: SERVICE_STATUS_UI_URI,
              mimeType: "application/json",
              text: JSON.stringify(structured),
            },
          },
        ],
      };
    } catch (e: any) {
      logToolCall("service_status", path, { modes }, SERVICE_STATUS_LOG_SCHEMA, e.message);
      return { content: [{ type: "text" as const, text: `Error fetching service status: ${e.message}` }], isError: true };
    }
  }
);

// --- journey ---
server.registerTool(
  "journey",
  {
    description: "Plan a journey between two points using the TfL Journey Planner. Can bridge different transport modes seamlessly.",
    inputSchema: z.object({
      from: z.string().describe("Origin: NaPTAN ID, postcode, or lat,lon"),
      to: z.string().describe("Destination: NaPTAN ID, postcode, or lat,lon"),
    }),
    annotations: { ...ANNOTATIONS, title: "Journey Planner" },
  },
  async ({ from, to }) => {
    try {
      const data = await httpGet(`/Journey/JourneyResults/${encodePath(from)}/to/${encodePath(to)}`);
      const parts: string[] = [];
      let journeyNum = 1;
      for (const journey of data.journeys ?? []) {
        const duration = journey.duration ?? 0;
        const legs = (journey.legs ?? []).map((leg: any) => {
          const summary = leg.instruction?.summary ?? leg.summary ?? "";
          const legDuration = leg.duration ?? 0;
          return `  - ${summary} (${legDuration} min)`;
        });
        parts.push(`Journey ${journeyNum++} (${duration} min):\n${legs.join("\n")}`);
      }
      return { content: [{ type: "text", text: parts.join("\n") }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching journey: ${e.message}` }], isError: true };
    }
  }
);

// --- crowding ---
server.registerTool(
  "crowding",
  {
    description: "Get live crowding data for a TfL station. Returns the current crowding level as a percentage of the typical baseline.",
    inputSchema: z.object({ stopName: z.string().describe("Stop name or search term, e.g. oxford") }),
    annotations: { ...ANNOTATIONS, title: "Station Crowding" },
  },
  async ({ stopName }) => {
    try {
      const naptan = await resolveStopName(stopName);
      const data = await httpGet(`/Crowding/${encodePath(naptan)}/Live`);
      if (!data.dataAvailable) {
        return { content: [{ type: "text", text: `Crowding data not available for ${naptan}` }] };
      }
      const pct: number = data.percentageOfBaseline;
      const rounded = Math.round(pct * 100);
      return { content: [{ type: "text", text: `${naptan}: ${rounded}% of typical crowding level (baseline ratio: ${pct})` }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching crowding data: ${e.message}` }], isError: true };
    }
  }
);

// --- fares ---
server.registerTool(
  "fares",
  {
    description: "Get fare information between two TfL stops, including pay-as-you-go and cash single prices for peak and off-peak travel.",
    inputSchema: z.object({
      fromName: z.string().describe("Origin stop name, e.g. oxford"),
      toName: z.string().describe("Destination stop name, e.g. bank"),
    }),
    annotations: { ...ANNOTATIONS, title: "Fares" },
  },
  async ({ fromName, toName }) => {
    try {
      const fromStopId = await resolveStopName(fromName);
      const toStopId = await resolveStopName(toName);
      const data: any[] = await httpGet(`/StopPoint/${encodePath(fromStopId)}/FareTo/${encodePath(toStopId)}`);
      const parts: string[] = [];
      for (const section of data) {
        for (const row of section.rows ?? []) {
          const from = row.from ?? "";
          const to = row.to ?? "";
          const passenger = row.passengerType ?? "Adult";
          const tickets = (row.ticketsAvailable ?? []).map((t: any) => {
            const type = t.ticketType ?? "";
            const time = t.ticketTime ?? "";
            const cost = t.cost ?? "";
            let line = `  £${cost} — ${type}`;
            if (time) line += ` (${time})`;
            return line;
          });
          parts.push(`${from} → ${to} (${passenger}):\n${tickets.join("\n")}`);
        }
      }
      return { content: [{ type: "text", text: parts.join("\n") }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching fares: ${e.message}` }], isError: true };
    }
  }
);

// --- bike_points ---
server.registerTool(
  "bike_points",
  {
    description: "Get TfL Santander Cycles bike point locations with available bikes and empty docks. Optionally filter by name with a search query.",
    inputSchema: z.object({ query: z.string().optional().describe("Optional name search, e.g. clerkenwell") }),
    annotations: { ...ANNOTATIONS, title: "Bike Points" },
  },
  async ({ query }) => {
    try {
      const path = query?.trim() ? `/BikePoint/Search/${encodePath(query.trim())}` : "/BikePoint";
      const data: any[] = await httpGet(path);
      const lines = data.map((point) => {
        const name = point.commonName ?? "";
        const id = point.id ?? "";
        let bikes = 0;
        let emptyDocks = 0;
        for (const prop of point.additionalProperties ?? []) {
          if (prop.key === "NbBikes") bikes = parseInt(prop.value, 10);
          if (prop.key === "NbEmptyDocks") emptyDocks = parseInt(prop.value, 10);
        }
        return `${name} (${id}): ${bikes} bikes available, ${emptyDocks} empty docks`;
      });
      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching bike points: ${e.message}` }], isError: true };
    }
  }
);

}

export function buildServer(): McpServer {
  const server = new McpServer(
    { name: "TfL", version: "2.0.0" },
    {
      cacheHints: {
        "server/discover": { ttlMs: 300_000, cacheScope: "public" },
        "tools/list": { ttlMs: 300_000, cacheScope: "public" },
        "resources/list": { ttlMs: 300_000, cacheScope: "public" },
        "resources/templates/list": { ttlMs: 300_000, cacheScope: "public" },
        "resources/read": { ttlMs: 300_000, cacheScope: "public" },
      },
    }
  );
  registerTflTools(server);
  return server;
}

// --- start server ---
async function main(): Promise<void> {
  if (process.argv.includes("--http")) {
    const { startHttpServer } = await import("./http.js");
    await startHttpServer();
    return;
  }

  console.error(`Starting TfL MCP Server v2.0.0...`);
  await serveStdio(buildServer, { legacy: "serve" });
  console.error("TfL MCP Server connected and ready");
}

const entryPoint = process.argv[1];
if (entryPoint !== undefined && import.meta.url === pathToFileURL(realpathSync(entryPoint)).href) {
  void main().catch((err) => {
    console.error("Fatal:", err);
    process.exitCode = 1;
  });
}
