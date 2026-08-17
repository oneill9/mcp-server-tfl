import {
  createServer,
  type IncomingMessage,
  type Server,
  type ServerResponse,
} from "node:http";
import {
  hostHeaderValidation,
  originValidation,
  toNodeHandler,
} from "@modelcontextprotocol/node";
import {
  createMcpHandler,
  type McpHttpHandler,
  type McpServerFactory,
  type PerRequestResponseMode,
} from "@modelcontextprotocol/server";
import { buildServer } from "./index.js";

const DEFAULT_HOST = "127.0.0.1";
const DEFAULT_PORT = 8080;
const DEFAULT_MAX_REQUEST_BODY_BYTES = 1_048_576;
const DEFAULT_REQUEST_TIMEOUT_MS = 30_000;
const DEFAULT_HEADERS_TIMEOUT_MS = 10_000;
const LOCAL_HOSTNAMES = ["localhost", "127.0.0.1", "[::1]"];

export type HttpServerOptions = {
  allowedHostnames?: string[];
  headersTimeoutMs?: number;
  maxRequestBodyBytes?: number;
  onHandler?: (handler: McpHttpHandler) => void;
  requestTimeoutMs?: number;
  responseMode?: PerRequestResponseMode;
  serverFactory?: McpServerFactory;
};

class RequestBodyTooLargeError extends Error {}

function positiveInteger(value: number, name: string): number {
  if (!Number.isSafeInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer`);
  }
  return value;
}

function configuredPositiveInteger(name: string, fallback: number): number {
  const rawValue = process.env[name];
  if (rawValue === undefined) return fallback;
  const value = Number(rawValue);
  return positiveInteger(value, name);
}

function jsonRpcError(
  response: ServerResponse,
  status: number,
  code: number,
  message: string
): void {
  response.writeHead(status, { "Content-Type": "application/json" });
  response.end(JSON.stringify({ jsonrpc: "2.0", error: { code, message }, id: null }));
}

async function readJsonBody(request: IncomingMessage, maxBytes: number): Promise<unknown> {
  const contentLength = request.headers["content-length"];
  if (contentLength !== undefined) {
    const declaredBytes = Number(contentLength);
    if (!Number.isSafeInteger(declaredBytes) || declaredBytes < 0) {
      request.resume();
      throw new SyntaxError("Invalid Content-Length header");
    }
    if (declaredBytes > maxBytes) {
      request.resume();
      throw new RequestBodyTooLargeError();
    }
  }

  return new Promise<unknown>((resolve, reject) => {
    const chunks: Buffer[] = [];
    let receivedBytes = 0;
    let settled = false;

    request.on("data", (chunk: Buffer | string) => {
      if (settled) return;
      const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      receivedBytes += buffer.byteLength;
      if (receivedBytes > maxBytes) {
        settled = true;
        chunks.length = 0;
        request.resume();
        reject(new RequestBodyTooLargeError());
        return;
      }
      chunks.push(buffer);
    });
    request.once("end", () => {
      if (settled) return;
      settled = true;
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString("utf8")));
      } catch (error) {
        reject(error);
      }
    });
    request.once("aborted", () => {
      if (settled) return;
      settled = true;
      reject(new Error("Request body was aborted"));
    });
    request.once("error", (error) => {
      if (settled) return;
      settled = true;
      reject(error);
    });
  });
}

function configuredAllowedHostnames(): string[] {
  const configured = process.env.MCP_ALLOWED_HOSTS
    ?.split(",")
    .map((hostname) => hostname.trim())
    .filter(Boolean);
  return configured && configured.length > 0 ? configured : LOCAL_HOSTNAMES;
}

export function createHttpServer(options: HttpServerOptions = {}): Server {
  const maxRequestBodyBytes = positiveInteger(
    options.maxRequestBodyBytes
      ?? configuredPositiveInteger("MCP_MAX_REQUEST_BODY_BYTES", DEFAULT_MAX_REQUEST_BODY_BYTES),
    "maxRequestBodyBytes"
  );
  const requestTimeoutMs = positiveInteger(
    options.requestTimeoutMs
      ?? configuredPositiveInteger("MCP_REQUEST_TIMEOUT_MS", DEFAULT_REQUEST_TIMEOUT_MS),
    "requestTimeoutMs"
  );
  const headersTimeoutMs = positiveInteger(
    options.headersTimeoutMs
      ?? configuredPositiveInteger("MCP_HEADERS_TIMEOUT_MS", DEFAULT_HEADERS_TIMEOUT_MS),
    "headersTimeoutMs"
  );
  if (headersTimeoutMs > requestTimeoutMs) {
    throw new Error("headersTimeoutMs must not exceed requestTimeoutMs");
  }

  const handler = createMcpHandler(options.serverFactory ?? buildServer, {
    legacy: "reject",
    responseMode: options.responseMode ?? "auto",
  });
  options.onHandler?.(handler);
  const nodeHandler = toNodeHandler(handler, {
    onerror: (error) => console.error("HTTP transport error:", error),
  });
  const allowedHostnames = options.allowedHostnames ?? configuredAllowedHostnames();
  const validateHost = hostHeaderValidation(allowedHostnames);
  const validateOrigin = originValidation(allowedHostnames);

  const server = createServer((request, response) => {
    const requestPath = new URL(request.url ?? "/", "http://localhost").pathname;
    if (requestPath !== "/mcp") {
      response.statusCode = 404;
      response.end("Not found");
      return;
    }
    if (!validateHost(request, response) || !validateOrigin(request, response)) return;

    if (request.method?.toUpperCase() !== "POST") {
      void nodeHandler(request, response);
      return;
    }

    void readJsonBody(request, maxRequestBodyBytes)
      .then((body) => nodeHandler(request, response, body))
      .catch((error: unknown) => {
        if (error instanceof RequestBodyTooLargeError) {
          jsonRpcError(
            response,
            413,
            -32000,
            `Request body exceeds ${maxRequestBodyBytes} bytes`
          );
          return;
        }
        jsonRpcError(response, 400, -32700, "Parse error");
      });
  });

  server.requestTimeout = requestTimeoutMs;
  server.headersTimeout = headersTimeoutMs;

  server.on("close", () => {
    void handler.close();
  });
  return server;
}

export async function startHttpServer(): Promise<Server> {
  const host = process.env.HOST ?? DEFAULT_HOST;
  const rawPort = process.env.PORT ?? String(DEFAULT_PORT);
  const port = Number.parseInt(rawPort, 10);
  if (!Number.isInteger(port) || port < 0 || port > 65_535) {
    throw new Error(`Invalid PORT: ${rawPort}`);
  }

  const server = createHttpServer();
  await new Promise<void>((resolve, reject) => {
    server.once("error", reject);
    server.listen(port, host, () => {
      server.off("error", reject);
      resolve();
    });
  });

  const address = server.address();
  const listeningPort = address !== null && typeof address !== "string" ? address.port : port;
  console.error(`TfL MCP Server listening on http://${host}:${listeningPort}/mcp`);
  return server;
}
