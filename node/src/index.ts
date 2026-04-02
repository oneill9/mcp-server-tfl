import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

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

const server = new McpServer({ name: "TfL", version: "1.2.0" });

// --- arrivals ---
server.tool(
  "arrivals",
  "Get live arrivals at a TfL stop. You should usually call the stop_search tool first to find the correct National Public Transport Access Node (NaPTAN) stopId.",
  { stopId: z.string().describe("NaPTAN stop ID, e.g. 940GZZLUOXC") },
  { ...ANNOTATIONS, title: "Live Arrivals" },
  async ({ stopId }) => {
    try {
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

// --- stop_search ---
server.tool(
  "stop_search",
  "Search for TfL stops by common name or search term. Use this tool first to lookup the stopId required by the arrivals tool.",
  { query: z.string().describe("Stop name or search term, e.g. oxford") },
  { ...ANNOTATIONS, title: "Stop Search" },
  async ({ query }) => {
    try {
      const data = await httpGet(`/StopPoint/Search/${encodePath(query)}`);
      const lines = (data.matches ?? []).map((m: any) => `${m.id} — ${m.name}`);
      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error searching stops: ${e.message}` }], isError: true };
    }
  }
);

// --- disruptions ---
server.tool(
  "disruptions",
  "Get current disruptions for one or more TfL public transport modes, e.g. tube, bus, overground, elizabeth-line, dlr.",
  { modes: z.string().describe("Comma-separated transport modes, e.g. tube,bus,overground,elizabeth-line") },
  { ...ANNOTATIONS, title: "Disruptions" },
  async ({ modes }) => {
    try {
      const data: any[] = await httpGet(`/Line/Mode/${encodeSegments(modes)}/Disruption`);
      const lines = data.map((d) => `${d.lineId}: ${d.description}`);
      return { content: [{ type: "text", text: lines.join("\n") }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching disruptions: ${e.message}` }], isError: true };
    }
  }
);

// --- line_status ---
server.tool(
  "line_status",
  "Get the current operational status and delays for one or more TfL tube, bus, or rail lines.",
  { lines: z.string().describe("Comma-separated line IDs, e.g. central,victoria,jubilee,elizabeth-line,overground") },
  { ...ANNOTATIONS, title: "Line Status" },
  async ({ lines }) => {
    try {
      const data: any[] = await httpGet(`/Line/${encodeSegments(lines)}/Status`);
      const result = data.map((line) => {
        const statuses = (line.lineStatuses ?? []).map((s: any) => {
          const desc = s.statusSeverityDescription ?? "";
          const reason = s.reason ?? "";
          return reason ? `${desc} — ${reason}` : desc;
        });
        return `${line.name}: ${statuses.join("; ")}`;
      });
      return { content: [{ type: "text", text: result.join("\n") }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching line status: ${e.message}` }], isError: true };
    }
  }
);

// --- journey ---
server.tool(
  "journey",
  "Plan a journey between two points using the TfL Journey Planner. Can bridge different transport modes seamlessly.",
  {
    from: z.string().describe("Origin: NaPTAN ID, postcode, or lat,lon"),
    to: z.string().describe("Destination: NaPTAN ID, postcode, or lat,lon"),
  },
  { ...ANNOTATIONS, title: "Journey Planner" },
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

// --- list_modes ---
server.tool(
  "list_modes",
  "Get a list of all valid TfL transport modes (e.g., tube, bus, dlr, overground).",
  {},
  { ...ANNOTATIONS, title: "List Transport Modes" },
  async () => {
    try {
      const data: any[] = await httpGet("/Line/Meta/Modes");
      const modes = data.map((m) => m.modeName);
      return { content: [{ type: "text", text: modes.join(", ") }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching modes: ${e.message}` }], isError: true };
    }
  }
);

// --- line_routes ---
server.tool(
  "line_routes",
  "Get the ordered sequence of stops along a TfL line in a given direction. Useful for seeing all stations on a line in order.",
  {
    lineId: z.string().describe("Line ID, e.g. central, victoria, northern"),
    direction: z.string().describe("Direction of travel: inbound or outbound"),
  },
  { ...ANNOTATIONS, title: "Line Routes" },
  async ({ lineId, direction }) => {
    try {
      const data = await httpGet(`/Line/${encodePath(lineId)}/Route/Sequence/${encodePath(direction)}`);
      const lineName = data.lineName ?? lineId;
      const stops: string[] = [];
      for (const sequence of data.stopPointSequences ?? []) {
        for (const stop of sequence.stopPoint ?? []) {
          stops.push(`  ${stop.name} (${stop.id})`);
        }
      }
      return { content: [{ type: "text", text: `${lineName} (${direction}):\n${stops.join("\n")}` }] };
    } catch (e: any) {
      return { content: [{ type: "text", text: `Error fetching line routes: ${e.message}` }], isError: true };
    }
  }
);

// --- crowding ---
server.tool(
  "crowding",
  "Get live crowding data for a TfL station. Returns the current crowding level as a percentage of the typical baseline. Use stop_search to find the NaPTAN ID first.",
  { naptan: z.string().describe("NaPTAN station ID, e.g. 940GZZLUOXC") },
  { ...ANNOTATIONS, title: "Station Crowding" },
  async ({ naptan }) => {
    try {
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
server.tool(
  "fares",
  "Get fare information between two TfL stops, including pay-as-you-go and cash single prices for peak and off-peak travel. Use stop_search to find stop IDs first.",
  {
    fromStopId: z.string().describe("Origin NaPTAN stop ID, e.g. 940GZZLUOXC"),
    toStopId: z.string().describe("Destination NaPTAN stop ID, e.g. 940GZZLUBND"),
  },
  { ...ANNOTATIONS, title: "Fares" },
  async ({ fromStopId, toStopId }) => {
    try {
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
server.tool(
  "bike_points",
  "Get TfL Santander Cycles bike point locations with available bikes and empty docks. Optionally filter by name with a search query.",
  { query: z.string().optional().describe("Optional name search, e.g. clerkenwell") },
  { ...ANNOTATIONS, title: "Bike Points" },
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

// --- start server ---
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((err) => {
  console.error("Fatal:", err);
  process.exit(1);
});
