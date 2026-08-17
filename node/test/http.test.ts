import { createServer, request as httpRequest, type Server } from "node:http";
import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { Client } from "@modelcontextprotocol/client";
import { StreamableHTTPClientTransport } from "@modelcontextprotocol/client";

const PROTOCOL_VERSION = "2026-07-28";

let mcpServer: Server;
let tflServer: Server;
let endpoint: URL;
let client: Client;
let transport: StreamableHTTPClientTransport;
const exchanges: Array<{ request: Request; response: Response }> = [];

async function listen(server: Server): Promise<number> {
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  if (address === null || typeof address === "string") {
    throw new Error("Expected the test server to listen on a TCP port");
  }
  return address.port;
}

async function close(server: Server | undefined): Promise<void> {
  if (server === undefined) return;
  await new Promise<void>((resolve, reject) =>
    server.close((error) => (error ? reject(error) : resolve()))
  );
}

beforeAll(async () => {
  tflServer = createServer((request, response) => {
    if (request.url === "/Line/Mode/tube/Status") {
      response.setHeader("Content-Type", "application/json");
      response.end(
        JSON.stringify([
          {
            id: "central",
            name: "Central",
            lineStatuses: [{ statusSeverityDescription: "Good Service" }],
          },
        ])
      );
      return;
    }

    response.statusCode = 404;
    response.end();
  });
  const tflPort = await listen(tflServer);
  process.env.TFL_BASE_URL = `http://127.0.0.1:${tflPort}`;

  const { createHttpServer } = await import("../src/http.js");
  mcpServer = createHttpServer({
    maxRequestBodyBytes: 2_048,
    requestTimeoutMs: 12_345,
    headersTimeoutMs: 6_789,
  });
  const mcpPort = await listen(mcpServer);
  endpoint = new URL(`http://127.0.0.1:${mcpPort}/mcp`);

  const recordingFetch: typeof fetch = async (input, init) => {
    const request = new Request(input, init);
    const response = await fetch(request);
    exchanges.push({ request, response: response.clone() });
    return response;
  };

  transport = new StreamableHTTPClientTransport(endpoint, { fetch: recordingFetch });
  client = new Client(
    { name: "http-acceptance-test", version: "1.0.0" },
    { versionNegotiation: { mode: { pin: PROTOCOL_VERSION } } }
  );
  await client.connect(transport);
}, 15_000);

afterAll(async () => {
  await client?.close();
  await close(mcpServer);
  await close(tflServer);
  delete process.env.TFL_BASE_URL;
});

describe("modern-only stateless Streamable HTTP", () => {
  it("configures bounded request and header timeouts", () => {
    expect(mcpServer.requestTimeout).toBe(12_345);
    expect(mcpServer.headersTimeout).toBe(6_789);
  });

  it("discovers the server at /mcp using the 2026-07-28 protocol", () => {
    expect(client.getProtocolEra()).toBe("modern");
    expect(client.getNegotiatedProtocolVersion()).toBe(PROTOCOL_VERSION);
    expect(client.getDiscoverResult()?.supportedVersions).toEqual([PROTOCOL_VERSION]);

    const discoverExchange = exchanges.find(
      ({ request }) => request.headers.get("mcp-method") === "server/discover"
    );
    expect(discoverExchange?.request.headers.get("mcp-protocol-version")).toBe(PROTOCOL_VERSION);
    expect(discoverExchange?.response.headers.get("mcp-session-id")).toBeNull();
  });

  it("returns a deterministic, cacheable tool list without a session", async () => {
    const first = await client.listTools(undefined, { cacheMode: "refresh" });
    const second = await client.listTools(undefined, { cacheMode: "refresh" });

    expect(first.tools.map(({ name }) => name)).toEqual([
      "arrivals",
      "service_status",
      "journey",
      "crowding",
      "fares",
      "bike_points",
    ]);
    expect(second.tools).toEqual(first.tools);
    expect(first.ttlMs).toBeGreaterThan(0);
    expect(first.cacheScope).toBe("public");

    const listExchanges = exchanges.filter(
      ({ request }) => request.headers.get("mcp-method") === "tools/list"
    );
    expect(listExchanges).toHaveLength(2);
    for (const { request, response } of listExchanges) {
      expect(request.headers.get("mcp-protocol-version")).toBe(PROTOCOL_VERSION);
      expect(response.headers.get("mcp-session-id")).toBeNull();
    }
  });

  it("calls a tool with required routing headers and structured output", async () => {
    const result = await client.callTool({
      name: "service_status",
      arguments: { modes: "tube" },
    });

    expect(result.isError).toBeFalsy();
    expect(result.structuredContent).toEqual([
      {
        id: "central",
        name: "Central",
        statuses: [{ severity: "Good Service" }],
      },
    ]);

    const toolExchange = exchanges.find(
      ({ request }) => request.headers.get("mcp-method") === "tools/call"
    );
    expect(toolExchange?.request.headers.get("mcp-name")).toBe("service_status");
    expect(toolExchange?.request.headers.get("mcp-protocol-version")).toBe(PROTOCOL_VERSION);
    expect(toolExchange?.response.headers.get("mcp-session-id")).toBeNull();
  });

  it("rejects requests whose routing headers disagree with the body", async () => {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        Accept: "application/json, text/event-stream",
        "Content-Type": "application/json",
        "MCP-Protocol-Version": PROTOCOL_VERSION,
        "Mcp-Method": "tools/list",
        "Mcp-Name": "arrivals",
      },
      body: JSON.stringify({
        jsonrpc: "2.0",
        id: 99,
        method: "tools/call",
        params: {
          name: "service_status",
          arguments: { modes: "tube" },
          _meta: {
            "io.modelcontextprotocol/protocolVersion": PROTOCOL_VERSION,
            "io.modelcontextprotocol/clientInfo": {
              name: "http-acceptance-test",
              version: "1.0.0",
            },
            "io.modelcontextprotocol/clientCapabilities": {},
          },
        },
      }),
    });

    expect(response.status).toBe(400);
    expect(await response.json()).toMatchObject({
      error: { code: -32020 },
    });
  });

  it("rejects legacy initialization", async () => {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        Accept: "application/json, text/event-stream",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        jsonrpc: "2.0",
        id: 100,
        method: "initialize",
        params: {
          protocolVersion: "2025-11-25",
          clientInfo: { name: "legacy-test", version: "1.0.0" },
          capabilities: {},
        },
      }),
    });

    expect(response.status).toBe(400);
    expect(response.headers.get("mcp-session-id")).toBeNull();
  });

  it("rejects an oversized Content-Length before buffering the request", async () => {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ payload: "x".repeat(4_096) }),
    });

    expect(response.status).toBe(413);
    expect(await response.json()).toMatchObject({
      error: { code: -32000, message: "Request body exceeds 2048 bytes" },
      id: null,
    });
  });

  it("rejects an oversized chunked body while it is being read", async () => {
    const response = await new Promise<{ status: number; body: string }>((resolve, reject) => {
      const request = httpRequest(endpoint, {
        method: "POST",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
      }, (incoming) => {
        const chunks: Buffer[] = [];
        incoming.on("data", (chunk) => chunks.push(Buffer.from(chunk)));
        incoming.on("end", () => resolve({
          status: incoming.statusCode ?? 0,
          body: Buffer.concat(chunks).toString("utf8"),
        }));
      });
      request.once("error", reject);
      request.write('{"payload":"');
      request.write("x".repeat(4_096));
      request.end('"}');
    });

    expect(response.status).toBe(413);
    expect(JSON.parse(response.body)).toMatchObject({
      error: { code: -32000, message: "Request body exceeds 2048 bytes" },
      id: null,
    });
  });

  it("composes production TfL registrations into the diagnostic conformance server", async () => {
    const { createHttpServer } = await import("../src/http.js");
    const { createConformanceServer } = await import("./support/conformance-server.js");
    const conformanceHttpServer = createHttpServer({ serverFactory: createConformanceServer });
    const conformancePort = await listen(conformanceHttpServer);
    const conformanceTransport = new StreamableHTTPClientTransport(
      new URL(`http://127.0.0.1:${conformancePort}/mcp`)
    );
    const conformanceClient = new Client(
      { name: "conformance-composition-test", version: "1.0.0" },
      { versionNegotiation: { mode: { pin: PROTOCOL_VERSION } } }
    );

    try {
      await conformanceClient.connect(conformanceTransport);
      const tools = await conformanceClient.listTools(undefined, { cacheMode: "refresh" });
      expect(tools.tools.map(({ name }) => name)).toEqual(expect.arrayContaining([
        "arrivals",
        "service_status",
        "journey",
        "crowding",
        "fares",
        "bike_points",
        "test_simple_text",
      ]));
    } finally {
      await conformanceClient.close();
      await close(conformanceHttpServer);
    }
  });
});
