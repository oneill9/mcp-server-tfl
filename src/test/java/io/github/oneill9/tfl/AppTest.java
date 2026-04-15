package io.github.oneill9.tfl;

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

        wireMock.stubFor(get(urlPathMatching("/Line/Mode/tube/Status"))
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

        wireMock.stubFor(get(urlPathMatching("/StopPoint/Search/bank"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "matches": [
                                    {"id":"940GZZLUBND","name":"Bond Street","lat":51.514,"lon":-0.149}
                                  ]
                                }
                                """)));

        wireMock.stubFor(get(urlPathMatching("/Crowding/940GZZLUOXC/Live"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"dataAvailable":true,"percentageOfBaseline":0.6863}
                                """)));

        wireMock.stubFor(get(urlPathMatching("/StopPoint/940GZZLUOXC/FareTo/940GZZLUBND"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "header":"Single Fare Finder",
                                    "rows":[{
                                      "from":"Oxford Circus","to":"Bond Street",
                                      "passengerType":"Adult",
                                      "ticketsAvailable":[
                                        {"ticketType":"Pay as you go","ticketTime":"Peak","cost":"2.80","mode":"tube"},
                                        {"ticketType":"Pay as you go","ticketTime":"Off Peak","cost":"2.70","mode":"tube"},
                                        {"ticketType":"CashSingle","ticketTime":"Anytime","cost":"6.70","mode":"tube"}
                                      ]
                                    }]
                                  }
                                ]
                                """)));

        wireMock.stubFor(get(urlPathMatching("/Line/Mode/unknown-mode/Status"))
                .willReturn(aResponse().withStatus(400)));

        wireMock.stubFor(get(urlPathMatching("/StopPoint/Search/notfound"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "matches": [] }
                                """)));

        wireMock.stubFor(get(urlPathMatching("/BikePoint"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "id":"BikePoints_1","commonName":"River Street, Clerkenwell",
                                    "lat":51.5292,"lon":-0.1097,
                                    "additionalProperties":[
                                      {"key":"NbBikes","value":"15"},
                                      {"key":"NbEmptyDocks","value":"8"}
                                    ]
                                  },
                                  {
                                    "id":"BikePoints_2","commonName":"Phillimore Gardens, Kensington",
                                    "lat":51.4991,"lon":-0.1984,
                                    "additionalProperties":[
                                      {"key":"NbBikes","value":"3"},
                                      {"key":"NbEmptyDocks","value":"14"}
                                    ]
                                  }
                                ]
                                """)));

        wireMock.stubFor(get(urlPathMatching("/BikePoint/Search/clerkenwell"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "id":"BikePoints_1","commonName":"River Street, Clerkenwell",
                                    "lat":51.5292,"lon":-0.1097,
                                    "additionalProperties":[
                                      {"key":"NbBikes","value":"15"},
                                      {"key":"NbEmptyDocks","value":"8"}
                                    ]
                                  }
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

        app = App.startHttp(0, "http://localhost:" + wireMock.port());

        var transport = HttpClientSseClientTransport.builder("http://localhost:" + app.getPort())
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
        assertEquals("TfL", info.name());
        var props = new java.util.Properties();
        try (var in = getClass().getResourceAsStream("/version.properties")) { props.load(in); } catch (Exception e) { throw new RuntimeException(e); }
        assertEquals(props.getProperty("version"), info.version());
    }

    @Test
    void serverHasToolCapability() {
        var caps = client.getServerCapabilities();
        assertNotNull(caps.tools());
    }

    // --- service_status ---

    @Test
    void listToolsContainsServiceStatus() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("service_status"), "Should contain service_status tool");
    }

    @Test
    void serviceStatusReturnsStatusForMode() {
        var result = client.callTool(new McpSchema.CallToolRequest("service_status", Map.of("modes", "tube")));
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
        var result = client.callTool(new McpSchema.CallToolRequest("arrivals", Map.of("stopName", "oxford")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("Central"), "Response should mention the line name");
        assertTrue(text.contains("Epping"), "Response should mention the destination");
        assertTrue(text.contains("2 min") || text.contains("120"), "Response should include time to station");
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



    // --- crowding ---

    @Test
    void listToolsContainsCrowding() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("crowding"), "Should contain crowding tool");
    }

    @Test
    void crowdingReturnsLiveData() {
        var result = client.callTool(new McpSchema.CallToolRequest("crowding",
                Map.of("stopName", "oxford")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("0.6863") || text.contains("68"), "Response should include crowding percentage");
    }

    // --- fares ---

    @Test
    void listToolsContainsFares() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("fares"), "Should contain fares tool");
    }

    @Test
    void faresReturnsFareBetweenStops() {
        var result = client.callTool(new McpSchema.CallToolRequest("fares",
                Map.of("fromName", "oxford", "toName", "bank")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("Oxford Circus"), "Response should mention origin");
        assertTrue(text.contains("Bond Street"), "Response should mention destination");
        assertTrue(text.contains("2.80") || text.contains("Peak"), "Response should include fare info");
    }

    // --- bike_points ---

    @Test
    void listToolsContainsBikePoints() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("bike_points"), "Should contain bike_points tool");
    }

    @Test
    void bikePointsReturnsAllStations() {
        var result = client.callTool(new McpSchema.CallToolRequest("bike_points", Map.of()));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("River Street"), "Response should mention station name");
        assertTrue(text.contains("bikes"), "Response should mention available bikes");
        assertTrue(text.contains("docks"), "Response should mention empty docks");
    }

    @Test
    void bikePointsSearchFiltersResults() {
        var result = client.callTool(new McpSchema.CallToolRequest("bike_points", Map.of("query", "clerkenwell")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("Clerkenwell"), "Response should match search query");
    }

    // --- tool annotations ---

    @Test
    void allToolsHaveRequiredAnnotations() {
        var result = client.listTools();
        var expectedTools = java.util.Set.of(
                "service_status", "arrivals", "journey", "crowding", "fares", "bike_points");
        for (McpSchema.Tool tool : result.tools()) {
            if (!expectedTools.contains(tool.name())) continue;
            assertNotNull(tool.annotations(), tool.name() + " should have annotations");
            assertNotNull(tool.annotations().title(),
                    tool.name() + " should have a title");
            assertFalse(tool.annotations().title().isBlank(),
                    tool.name() + " title should not be blank");
            assertEquals(Boolean.TRUE, tool.annotations().readOnlyHint(),
                    tool.name() + " should have readOnlyHint=true");
            assertEquals(Boolean.FALSE, tool.annotations().destructiveHint(),
                    tool.name() + " should have destructiveHint=false");
        }
    }

    // --- error handling ---

    @Test
    void serviceStatusReturnsErrorOn400() {
        var result = client.callTool(new McpSchema.CallToolRequest("service_status", Map.of("modes", "unknown-mode")));
        assertTrue(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("400"), "Error message should mention the HTTP status code");
    }

    @Test
    void arrivalsReturnsErrorOnNotFound() {
        var result = client.callTool(new McpSchema.CallToolRequest("arrivals", Map.of("stopName", "notfound")));
        assertTrue(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("No stop found"), "Error message should mention No stop found");
    }
}
