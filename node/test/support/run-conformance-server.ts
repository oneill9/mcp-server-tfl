import { createHttpServer } from "../../src/http.js";
import {
  configureConformanceNotifications,
  createConformanceServer,
} from "./conformance-server.js";

const rawPort = process.env.PORT ?? "3000";
const port = Number.parseInt(rawPort, 10);
if (!Number.isInteger(port) || port < 1 || port > 65_535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

const server = createHttpServer({
  serverFactory: createConformanceServer,
  responseMode: "auto",
  onHandler: ({ notify }) => {
    configureConformanceNotifications({
      toolsChanged: () => void notify.toolsChanged(),
      promptsChanged: () => void notify.promptsChanged(),
    });
  },
});

server.listen(port, "127.0.0.1", () => {
  console.error(`TfL MCP conformance fixture listening on http://127.0.0.1:${port}/mcp`);
});

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.once(signal, () => {
    server.close(() => process.exit(0));
  });
}
