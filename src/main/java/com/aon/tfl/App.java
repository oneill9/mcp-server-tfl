package com.aon.tfl;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.util.List;
import java.util.Map;

public class App {

    private final Server jetty;
    private final McpSyncServer mcpServer;
    private final int port;

    public App(int port) throws Exception {
        this.port = port;

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

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "3001"));
        App app = new App(port);
        app.start();
        app.jetty.join();
    }
}
