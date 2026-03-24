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

        wireMock.stubFor(get(urlPathMatching("/StopPoint/940GZZLUOXC/Arrivals"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"lineName":"Central","platformName":"Eastbound - Platform 2","destinationName":"Epping","timeToStation":120},
                                  {"lineName":"Central","platformName":"Eastbound - Platform 2","destinationName":"Epping","timeToStation":300}
                                ]
                                """)));

        wireMock.stubFor(get(urlPathMatching("/StopPoint/Search/oxford"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "matches": [
                                    {"id":"940GZZLUOXC","name":"Oxford Circus Underground Station","lat":51.515,"lon":-0.1416},
                                    {"id":"490000173RC","name":"Oxford Circus","lat":51.5148,"lon":-0.1418}
                                  ]
                                }
                                """)));

        wireMock.stubFor(get(urlPathMatching("/Line/Mode/tube/Disruption"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"lineId":"central","description":"Minor delays due to earlier signal failure near Oxford Circus"},
                                  {"lineId":"jubilee","description":"Good service"}
                                ]
                                """)));

        wireMock.stubFor(get(urlPathMatching("/BikePoint"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"id":"BikePoints_1","commonName":"River Street, Clerkenwell","lat":51.5292,"lon":-0.1086,"additionalProperties":[{"key":"NbBikes","value":"9"},{"key":"NbEmptyDocks","value":"9"}]},
                                  {"id":"BikePoints_2","commonName":"Phillimore Gardens, Kensington","lat":51.4996,"lon":-0.1975,"additionalProperties":[{"key":"NbBikes","value":"0"},{"key":"NbEmptyDocks","value":"13"}]}
                                ]
                                """)));

        wireMock.stubFor(get(urlPathMatching("/Journey/JourneyResults/1000123/to/1000456"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "journeys": [
                                    {
                                      "duration": 25,
                                      "legs": [
                                        {
                                          "summary": "Take Central line to Bank",
                                          "duration": 10,
                                          "instruction": {"summary": "Take Central line to Bank"}
                                        },
                                        {
                                          "summary": "Walk to destination",
                                          "duration": 5,
                                          "instruction": {"summary": "Walk to destination"}
                                        }
                                      ]
                                    },
                                    {
                                      "duration": 35,
                                      "legs": [
                                        {
                                          "summary": "Take bus 23 to Liverpool Street",
                                          "duration": 20,
                                          "instruction": {"summary": "Take bus 23 to Liverpool Street"}
                                        }
                                      ]
                                    }
                                  ]
                                }
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

    // --- arrivals ---

    @Test
    void listToolsContainsArrivals() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("arrivals"), "Should contain arrivals tool");
    }

    @Test
    void arrivalsReturnsLiveArrivals() {
        var result = client.callTool(new McpSchema.CallToolRequest("arrivals", Map.of("stopId", "940GZZLUOXC")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("Central"), "Response should mention the line name");
        assertTrue(text.contains("Epping"), "Response should mention the destination");
        assertTrue(text.contains("2 min") || text.contains("120"), "Response should include time to station");
    }

    // --- stop_search ---

    @Test
    void listToolsContainsStopSearch() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("stop_search"), "Should contain stop_search tool");
    }

    @Test
    void stopSearchReturnsMatchingStops() {
        var result = client.callTool(new McpSchema.CallToolRequest("stop_search", Map.of("query", "oxford")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("Oxford Circus"), "Response should mention Oxford Circus");
        assertTrue(text.contains("940GZZLUOXC"), "Response should include the stop ID");
    }

    // --- disruptions ---

    @Test
    void listToolsContainsDisruptions() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("disruptions"), "Should contain disruptions tool");
    }

    @Test
    void disruptionsReturnsDisruptionsByMode() {
        var result = client.callTool(new McpSchema.CallToolRequest("disruptions", Map.of("modes", "tube")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("central"), "Response should mention the affected line");
        assertTrue(text.contains("signal failure") || text.contains("Minor delays"), "Response should include disruption description");
    }

    @Test
    void lineStatusAcceptsMultipleLines() {
        var result = client.callTool(new McpSchema.CallToolRequest("line_status", Map.of("lines", "central,victoria")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.toLowerCase().contains("central"), "Response should mention Central line");
        assertTrue(text.toLowerCase().contains("victoria"), "Response should mention Victoria line");
    }

    // --- journey ---

    @Test
    void listToolsContainsJourney() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("journey"), "Should contain journey tool");
    }

    @Test
    void journeyReturnsPlanBetweenTwoPoints() {
        var result = client.callTool(new McpSchema.CallToolRequest("journey", Map.of("from", "1000123", "to", "1000456")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("25") || text.contains("Central"), "Response should include journey details");
        assertTrue(text.contains("Bank") || text.contains("leg") || text.contains("min"), "Response should include leg summary");
    }

    // --- bike_points ---

    @Test
    void listToolsContainsBikePoints() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("bike_points"), "Should contain bike_points tool");
    }

    @Test
    void bikePointsReturnsDockingStations() {
        var result = client.callTool(new McpSchema.CallToolRequest("bike_points", Map.of()));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("Clerkenwell") || text.contains("River Street"), "Response should mention a docking station name");
        assertTrue(text.contains("9") || text.contains("BikePoints_1"), "Response should include bike availability info");
    }
}
