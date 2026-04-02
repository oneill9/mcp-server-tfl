package io.github.oneill9.tfl;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

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
    static McpSyncClient client;

    @BeforeAll
    static void setUp() throws Exception {
        app = App.startHttp(0, "https://api.tfl.gov.uk");

        var transport = HttpClientSseClientTransport.builder("http://localhost:" + app.getPort())
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
    void lineRoutesReturnsRealStops() {
        var result = client.callTool(new McpSchema.CallToolRequest("line_routes",
                Map.of("lineId", "central", "direction", "outbound")));
        assertFalse(result.isError(), "line_routes should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "line_routes should return a non-empty response");
        assertTrue(text.contains("Central"), "Response should mention the Central line");
    }

    @Test
    void crowdingReturnsRealData() {
        // 940GZZLUOXC = Oxford Circus Underground Station
        var result = client.callTool(new McpSchema.CallToolRequest("crowding",
                Map.of("naptan", "940GZZLUOXC")));
        assertFalse(result.isError(), "crowding should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "crowding should return a non-empty response");
    }

    @Test
    void faresReturnsRealData() {
        // Oxford Circus to Bond Street
        var result = client.callTool(new McpSchema.CallToolRequest("fares",
                Map.of("fromStopId", "940GZZLUOXC", "toStopId", "940GZZLUBND")));
        assertFalse(result.isError(), "fares should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "fares should return a non-empty response");
        assertTrue(text.contains("Oxford Circus") || text.contains("Bond Street"),
                "Response should mention the stations");
    }
}
