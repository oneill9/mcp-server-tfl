package com.aon.tfl;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class App {

    private static final String TFL_APP_KEY = System.getenv("TFL_APP_KEY");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Server jetty;
    private final McpSyncServer mcpServer;
    private final int port;
    private final String tflBase;

    public App(int port) throws Exception {
        this(port, "https://api.tfl.gov.uk");
    }

    public App(int port, String tflBase) throws Exception {
        this.port = port;
        this.tflBase = tflBase;

        var transportProvider = HttpServletSseServerTransportProvider.builder()
                .messageEndpoint("/mcp/message")
                .sseEndpoint("/sse")
                .build();

        mcpServer = McpServer.sync(transportProvider)
                .serverInfo("tfl-server", "0.1.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("echo")
                                .description("Echoes back the provided text")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of("text", Map.of("type", "string", "description", "Text to echo back")),
                                        List.of("text"),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> McpSchema.CallToolResult.builder()
                                .addTextContent(request.arguments().get("text").toString())
                                .build())
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("greet")
                                .description("Returns a greeting for the given name")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of("name", Map.of("type", "string", "description", "Name to greet")),
                                        List.of("name"),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> McpSchema.CallToolResult.builder()
                                .addTextContent("Hello, " + request.arguments().get("name") + "! Welcome to TFL.")
                                .build())
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("line_status")
                                .description("Get the current status of one or more TfL lines (e.g. central, victoria, jubilee)")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of("lines", Map.of("type", "string", "description", "Comma-separated line IDs, e.g. central,victoria")),
                                        List.of("lines"),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            String lines = request.arguments().get("lines").toString();
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchLineStatus(lines))
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching line status: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .build();

        jetty = new Server();
        ServerConnector connector = new ServerConnector(jetty);
        connector.setPort(port);
        jetty.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(transportProvider), "/*");
        jetty.setHandler(context);
    }

    public Server getJetty() {
        return jetty;
    }

    public void start() throws Exception {
        jetty.start();
        System.out.println("MCP server running on http://localhost:" + port);
        System.out.println("  SSE endpoint: /sse");
        System.out.println("  Message endpoint: /mcp/message");
    }

    public void stop() throws Exception {
        mcpServer.closeGracefully();
        jetty.stop();
    }

    private String fetchLineStatus(String lines) throws Exception {
        String url = tflBase + "/Line/" + lines + "/Status";
        if (TFL_APP_KEY != null && !TFL_APP_KEY.isBlank()) {
            url += "?app_key=" + TFL_APP_KEY;
        }
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());

        var sb = new StringBuilder();
        for (JsonNode line : root) {
            String name = line.path("name").asText();
            var statuses = new ArrayList<String>();
            for (JsonNode status : line.path("lineStatuses")) {
                String desc = status.path("statusSeverityDescription").asText();
                String reason = status.path("reason").asText("");
                statuses.add(reason.isBlank() ? desc : desc + " — " + reason);
            }
            sb.append(name).append(": ").append(String.join("; ", statuses)).append("\n");
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "3001"));
        App app = new App(port);
        app.start();
        app.jetty.join();
    }
}
