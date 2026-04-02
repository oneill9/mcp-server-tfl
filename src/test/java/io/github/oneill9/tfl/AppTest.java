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

        wireMock.stubFor(get(urlPathMatching("/Line/Meta/Modes"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"modeName":"tube","isTflService":true},
                                  {"modeName":"bus","isTflService":true},
                                  {"modeName":"dlr","isTflService":true}
                                ]
                                """)));

        wireMock.stubFor(get(urlPathMatching("/Line/central/Route/Sequence/outbound"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "lineId":"central","lineName":"Central","direction":"outbound",
                                  "stopPointSequences":[{
                                    "branchId":0,
                                    "stopPoint":[
                                      {"id":"940GZZLUEPG","name":"Epping Underground Station","lat":51.6937,"lon":0.1139},
                                      {"id":"940GZZLUTHB","name":"Theydon Bois Underground Station","lat":51.6717,"lon":0.1033},
                                      {"id":"940GZZLUOXC","name":"Oxford Circus Underground Station","lat":51.515,"lon":-0.1416}
                                    ]
                                  }]
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

        wireMock.stubFor(get(urlPathMatching("/Line/bad-line/Status"))
                .willReturn(aResponse().withStatus(500)));

        wireMock.stubFor(get(urlPathMatching("/StopPoint/notfound/Arrivals"))
                .willReturn(aResponse().withStatus(404)));

        wireMock.stubFor(get(urlPathMatching("/Line/Mode/unknown-mode/Disruption"))
                .willReturn(aResponse().withStatus(400)));

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

    // --- list_modes ---

    @Test
    void listToolsContainsListModes() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("list_modes"), "Should contain list_modes tool");
    }

    @Test
    void listModesReturnsAvailableModes() {
        var result = client.callTool(new McpSchema.CallToolRequest("list_modes", Map.of()));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("tube"), "Response should mention tube");
        assertTrue(text.contains("bus"), "Response should mention bus");
        assertTrue(text.contains("dlr"), "Response should mention dlr");
    }

    // --- line_routes ---

    @Test
    void listToolsContainsLineRoutes() {
        var result = client.listTools();
        var names = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("line_routes"), "Should contain line_routes tool");
    }

    @Test
    void lineRoutesReturnsStopsInOrder() {
        var result = client.callTool(new McpSchema.CallToolRequest("line_routes",
                Map.of("lineId", "central", "direction", "outbound")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("Central"), "Response should mention the line name");
        assertTrue(text.contains("Epping"), "Response should include a station on the route");
        assertTrue(text.contains("Oxford Circus"), "Response should include another station");
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
                Map.of("naptan", "940GZZLUOXC")));
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
                Map.of("fromStopId", "940GZZLUOXC", "toStopId", "940GZZLUBND")));
        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("Oxford Circus"), "Response should mention origin");
        assertTrue(text.contains("Bond Street"), "Response should mention destination");
        assertTrue(text.contains("2.80") || text.contains("Peak"), "Response should include fare info");
    }

    // --- tool annotations ---

    @Test
    void allToolsHaveRequiredAnnotations() {
        var result = client.listTools();
        var expectedTools = java.util.Set.of(
                "line_status", "arrivals", "stop_search", "disruptions",
                "journey", "list_modes", "line_routes", "crowding", "fares");
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
    void lineStatusReturnsErrorOn500() {
        var result = client.callTool(new McpSchema.CallToolRequest("line_status", Map.of("lines", "bad-line")));
        assertTrue(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("500"), "Error message should mention the HTTP status code");
    }

    @Test
    void arrivalsReturnsErrorOn404() {
        var result = client.callTool(new McpSchema.CallToolRequest("arrivals", Map.of("stopId", "notfound")));
        assertTrue(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("404"), "Error message should mention the HTTP status code");
    }

    @Test
    void disruptionsReturnsErrorOn400() {
        var result = client.callTool(new McpSchema.CallToolRequest("disruptions", Map.of("modes", "unknown-mode")));
        assertTrue(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("400"), "Error message should mention the HTTP status code");
    }
}
