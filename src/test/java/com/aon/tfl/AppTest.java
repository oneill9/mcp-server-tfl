package com.aon.tfl;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    static App app;
    static McpSyncClient client;

    @BeforeAll
    static void setUp() throws Exception {
        app = new App(0); // port 0 = random available port
        app.start();

        // Get the actual port Jetty chose
        int port = ((org.eclipse.jetty.server.ServerConnector)
                app.getJetty().getConnectors()[0]).getLocalPort();

        var transport = HttpClientSseClientTransport.builder("http://localhost:" + port)
                .sseEndpoint("/sse")
                .build();

        client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        client.initialize();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (client != null) client.close();
        if (app != null) app.stop();
    }

    @Test
    void serverReportsCorrectInfo() {
        var info = client.getServerInfo();
        assertEquals("tfl-server", info.name());
        assertEquals("0.1.0", info.version());
    }

    @Test
    void serverHasToolCapability() {
        var caps = client.getServerCapabilities();
        assertNotNull(caps.tools());
    }

    @Test
    void listToolsReturnsEchoAndGreet() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("echo"), "Should contain echo tool");
        assertTrue(names.contains("greet"), "Should contain greet tool");
        assertEquals(2, names.size());
    }

    @Test
    void echoToolReturnsInput() {
        var result = client.callTool(new McpSchema.CallToolRequest("echo", Map.of("text", "hello world")));
        assertFalse(result.isError());
        assertEquals(1, result.content().size());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertEquals("hello world", text);
    }

    @Test
    void greetToolReturnsGreeting() {
        var result = client.callTool(new McpSchema.CallToolRequest("greet", Map.of("name", "Alice")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertEquals("Hello, Alice! Welcome to TFL.", text);
    }
}
