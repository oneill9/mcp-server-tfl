package com.aon.tfl;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    static WireMockServer wireMock;
    static App app;
    static McpSyncClient client;

    @BeforeAll
    static void setUp() throws Exception {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        wireMock.stubFor(get(urlPathMatching("/Line/central/Status"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"id":"central","name":"Central","lineStatuses":[{"statusSeverityDescription":"Good Service","reason":""}]}]
                                """)));

        wireMock.stubFor(get(urlPathMatching("/Line/central,victoria/Status"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"id":"central","name":"Central","lineStatuses":[{"statusSeverityDescription":"Good Service","reason":""}]},
                                  {"id":"victoria","name":"Victoria","lineStatuses":[{"statusSeverityDescription":"Minor Delays","reason":"Earlier signal failure"}]}
                                ]
                                """)));

        app = new App(0, "http://localhost:" + wireMock.port());
        app.start();

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
        if (wireMock != null) wireMock.stop();
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
    void listToolsContainsEchoAndGreet() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("echo"), "Should contain echo tool");
        assertTrue(names.contains("greet"), "Should contain greet tool");
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

    // --- line_status ---

    @Test
    void listToolsContainsLineStatus() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("line_status"), "Should contain line_status tool");
    }

    @Test
    void lineStatusReturnsStatusForCentralLine() {
        var result = client.callTool(new McpSchema.CallToolRequest("line_status", Map.of("lines", "central")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.toLowerCase().contains("central"), "Response should mention Central line");
        assertTrue(text.contains("Good Service"), "Response should contain the status");
    }

    @Test
    void lineStatusAcceptsMultipleLines() {
        var result = client.callTool(new McpSchema.CallToolRequest("line_status", Map.of("lines", "central,victoria")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.toLowerCase().contains("central"), "Response should mention Central line");
        assertTrue(text.toLowerCase().contains("victoria"), "Response should mention Victoria line");
    }
}
