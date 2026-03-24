package com.tfl;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests that exercise each MCP tool against the real TfL API.
 * These tests require network access and optionally a TFL_APP_KEY env var for
 * higher rate limits.
 *
 * Run with: ./gradlew contractTest
 */
@Tag("contract")
class ContractTest {

    static App app;
    static Server jetty;
    static McpSyncClient client;

    @BeforeAll
    static void setUp() throws Exception {
        var transportProvider = HttpServletSseServerTransportProvider.builder()
                .messageEndpoint("/mcp/message")
                .sseEndpoint("/sse")
                .build();

        app = new App(transportProvider, "https://api.tfl.gov.uk");

        jetty = new Server();
        ServerConnector connector = new ServerConnector(jetty);
        connector.setPort(0);
        jetty.addConnector(connector);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(transportProvider), "/*");
        jetty.setHandler(context);
        jetty.start();

        int port = ((ServerConnector) jetty.getConnectors()[0]).getLocalPort();

        var transport = HttpClientSseClientTransport.builder("http://localhost:" + port)
                .sseEndpoint("/sse")
                .build();

        client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();

        client.initialize();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (client != null) client.close();
        if (app != null) app.stop();
        if (jetty != null) jetty.stop();
    }

    @Test
    void listModesReturnsRealModes() {
        var result = client.callTool(new McpSchema.CallToolRequest("list_modes", Map.of()));
        assertFalse(result.isError(), "list_modes should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "list_modes should return a non-empty response");
        assertTrue(text.contains("tube"), "TfL should always include tube as a mode");
        assertTrue(text.contains("bus"), "TfL should always include bus as a mode");
    }

    @Test
    void lineStatusReturnsRealStatus() {
        var result = client.callTool(new McpSchema.CallToolRequest("line_status", Map.of("lines", "central")));
        assertFalse(result.isError(), "line_status should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "line_status should return a non-empty response");
        assertTrue(text.toLowerCase().contains("central"), "Response should mention the Central line");
    }

    @Test
    void lineStatusAcceptsMultipleLines() {
        var result = client.callTool(new McpSchema.CallToolRequest("line_status",
                Map.of("lines", "central,victoria,jubilee")));
        assertFalse(result.isError(), "line_status should not return an error for multiple lines");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.toLowerCase().contains("central"), "Response should mention Central");
        assertTrue(text.toLowerCase().contains("victoria"), "Response should mention Victoria");
        assertTrue(text.toLowerCase().contains("jubilee"), "Response should mention Jubilee");
    }

    @Test
    void stopSearchReturnsRealStops() {
        var result = client.callTool(new McpSchema.CallToolRequest("stop_search",
                Map.of("query", "oxford circus")));
        assertFalse(result.isError(), "stop_search should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "stop_search should return a non-empty response");
        assertTrue(text.toLowerCase().contains("oxford"), "Response should mention Oxford Circus");
        assertTrue(text.contains("940GZZLUOXC"), "Response should include the Oxford Circus NaPTAN ID");
    }

    @Test
    void arrivalsReturnsRealData() {
        // 940GZZLUOXC = Oxford Circus Underground Station
        // Arrivals may legitimately be empty outside service hours — just assert no error
        var result = client.callTool(new McpSchema.CallToolRequest("arrivals",
                Map.of("stopId", "940GZZLUOXC")));
        assertFalse(result.isError(), "arrivals should not return an error");
    }

    @Test
    void disruptionsReturnsRealData() {
        var result = client.callTool(new McpSchema.CallToolRequest("disruptions",
                Map.of("modes", "tube")));
        assertFalse(result.isError(), "disruptions should not return an error");
        // May legitimately be empty when the tube is running normally
    }

    @Test
    void bikePointsReturnsRealStations() {
        var result = client.callTool(new McpSchema.CallToolRequest("bike_points", Map.of()));
        assertFalse(result.isError(), "bike_points should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "bike_points should return a non-empty response");
        assertTrue(text.contains("BikePoints_"), "Response should include Santander Cycles station IDs");
    }

    @Test
    void journeyReturnsPlan() {
        // Victoria station (51.4952,-0.1441) to Liverpool Street (51.5179,-0.0816)
        var result = client.callTool(new McpSchema.CallToolRequest("journey", Map.of(
                "from", "51.4952,-0.1441",
                "to",   "51.5179,-0.0816")));
        assertFalse(result.isError(), "journey should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "journey should return a non-empty response");
        assertTrue(text.contains("Journey 1"), "Response should include at least one journey option");
    }

    @Test
    void airQualityReturnsRealData() {
        var result = client.callTool(new McpSchema.CallToolRequest("air_quality", Map.of()));
        assertFalse(result.isError(), "air_quality should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "air_quality should return a non-empty response");
    }

    @Test
    void roadDisruptionsReturnsRealData() {
        var result = client.callTool(new McpSchema.CallToolRequest("road_disruptions", Map.of()));
        assertFalse(result.isError(), "road_disruptions should not return an error");
        // May legitimately be empty when roads are clear
    }
}
