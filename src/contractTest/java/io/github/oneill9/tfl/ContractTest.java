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
    void serviceStatusReturnsRealStatus() {
        var result = client.callTool(new McpSchema.CallToolRequest("service_status", Map.of("modes", "tube")));
        assertFalse(result.isError(), "service_status should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "service_status should return a non-empty response");
        assertTrue(text.toLowerCase().contains("central"), "Response should mention the Central line");
    }

    @Test
    void serviceStatusAcceptsMultipleModes() {
        var result = client.callTool(new McpSchema.CallToolRequest("service_status",
                Map.of("modes", "tube,overground,elizabeth-line,dlr")));
        assertFalse(result.isError(), "service_status should not return an error for multiple modes");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "service_status should return a non-empty response");

        var embedded = result.content().stream()
                .filter(McpSchema.EmbeddedResource.class::isInstance)
                .map(McpSchema.EmbeddedResource.class::cast)
                .findFirst()
                .orElseThrow();
        var resource = (McpSchema.TextResourceContents) embedded.resource();
        assertEquals("application/json", resource.mimeType());
        assertFalse(resource.text().contains("<html"), "Structured data should not be HTML");
        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(resource.text()));
    }

    @Test
    void arrivalsReturnsRealData() {
        var result = client.callTool(new McpSchema.CallToolRequest("arrivals",
                Map.of("stopName", "Oxford Circus Underground Station")));
        assertFalse(result.isError(), "arrivals should not return an error");
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
    void crowdingReturnsRealData() {
        var result = client.callTool(new McpSchema.CallToolRequest("crowding",
                Map.of("stopName", "Oxford Circus Underground Station")));
        assertFalse(result.isError(), "crowding should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "crowding should return a non-empty response");
    }

    @Test
    void faresReturnsRealData() {
        var result = client.callTool(new McpSchema.CallToolRequest("fares",
                Map.of("fromName", "Oxford Circus Underground Station", "toName", "Bond Street Underground Station")));
        assertFalse(result.isError(), "fares should not return an error");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "fares should return a non-empty response");
        assertTrue(text.contains("Oxford Circus") || text.contains("Bond Street"),
                "Response should mention the stations");
    }
}
