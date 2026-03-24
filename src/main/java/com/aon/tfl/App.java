package com.aon.tfl;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static final String TFL_APP_KEY = System.getenv("TFL_APP_KEY");
    private static final String TFL_APP_ID = System.getenv("TFL_APP_ID");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String VERSION = loadVersion();

    private static String loadVersion() {
        try (InputStream in = App.class.getResourceAsStream("/version.properties")) {
            if (in == null) return "dev";
            var props = new Properties();
            props.load(in);
            return props.getProperty("version", "dev");
        } catch (Exception e) {
            return "dev";
        }
    }

    private final McpSyncServer mcpServer;
    private final String tflBase;

    public App(McpServerTransportProvider transportProvider, String tflBase) {
        this.tflBase = tflBase;

        mcpServer = McpServer.sync(transportProvider)
                .serverInfo("tfl-server", VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("arrivals")
                                .description("Get live arrivals at a TfL stop. You should usually call the stop_search tool first to find the correct National Public Transport Access Node (NaPTAN) stopId.")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of("stopId", Map.of("type", "string", "description", "NaPTAN stop ID, e.g. 940GZZLUOXC")),
                                        List.of("stopId"),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            String stopId = request.arguments().get("stopId").toString();
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchArrivals(stopId))
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching arrivals: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("stop_search")
                                .description("Search for TfL stops by common name or search term. Use this tool first to lookup the stopId required by the arrivals tool.")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of("query", Map.of("type", "string", "description", "Stop name or search term, e.g. oxford")),
                                        List.of("query"),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            String query = request.arguments().get("query").toString();
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchStopSearch(query))
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error searching stops: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("disruptions")
                                .description("Get current disruptions for one or more TfL transport modes. Call the list_modes tool to see all valid modes.")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of("modes", Map.of("type", "string", "description", "Comma-separated transport modes, e.g. tube,bus,dlr,overground")),
                                        List.of("modes"),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            String modes = request.arguments().get("modes").toString();
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchDisruptions(modes))
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching disruptions: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("line_status")
                                .description("Get the current operational status and delays for one or more TfL lines.")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of("lines", Map.of("type", "string", "description", "Comma-separated line IDs, e.g. central,victoria,circle,dlr")),
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
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("bike_points")
                                .description("List all Santander Cycles docking stations across London with currently available bikes and empty docks.")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of(),
                                        List.of(),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchBikePoints())
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching bike points: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("journey")
                                .description("Plan a journey between two points using the TfL Journey Planner. Can bridge different transport modes seamlessly.")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of(
                                                "from", Map.of("type", "string", "description", "Origin: NaPTAN ID, postcode, or lat,lon"),
                                                "to",   Map.of("type", "string", "description", "Destination: NaPTAN ID, postcode, or lat,lon")),
                                        List.of("from", "to"),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            String from = request.arguments().get("from").toString();
                            String to   = request.arguments().get("to").toString();
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchJourney(from, to))
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching journey: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("list_modes")
                                .description("Get a list of all valid TfL transport modes (e.g., tube, bus, dlr, overground).")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of(),
                                        List.of(),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchListModes())
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching modes: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("air_quality")
                                .description("Get the latest London air quality data feed.")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of(),
                                        List.of(),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchAirQuality())
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching air quality: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("road_disruptions")
                                .description("Get a list of disrupted streets and A-roads in London.")
                                .inputSchema(new McpSchema.JsonSchema(
                                        "object",
                                        Map.of(),
                                        List.of(),
                                        null, null, null))
                                .build(),
                        (exchange, request) -> {
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchRoadDisruptions())
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching road disruptions: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .build();
    }

    public void stop() throws Exception {
        mcpServer.closeGracefully();
    }

    private String withAuth(String url) {
        var params = new ArrayList<String>();
        if (TFL_APP_ID != null && !TFL_APP_ID.isBlank()) params.add("app_id=" + TFL_APP_ID);
        if (TFL_APP_KEY != null && !TFL_APP_KEY.isBlank()) params.add("app_key=" + TFL_APP_KEY);
        if (params.isEmpty()) return url;
        return url + "?" + String.join("&", params);
    }

    private String fetchListModes() throws Exception {
        String url = withAuth(tflBase + "/Line/Meta/Modes");
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());

        var modes = new ArrayList<String>();
        for (JsonNode mode : root) {
            modes.add(mode.path("modeName").asText());
        }
        return String.join(", ", modes);
    }

    private String fetchAirQuality() throws Exception {
        String url = withAuth(tflBase + "/AirQuality");
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());
        return root.toPrettyString();
    }

    private String fetchRoadDisruptions() throws Exception {
        String url = withAuth(tflBase + "/Road/all/Street/Disruption");
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());

        var sb = new StringBuilder();
        for (JsonNode disruption : root) {
            String location = disruption.path("location").asText("Unknown Location");
            String comments = disruption.path("comments").asText("");
            sb.append(location).append(": ").append(comments).append("\n");
        }
        return sb.toString().trim();
    }

    private String fetchDisruptions(String modes) throws Exception {
        String url = withAuth(tflBase + "/Line/Mode/" + modes + "/Disruption");
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());

        var sb = new StringBuilder();
        for (JsonNode disruption : root) {
            String lineId = disruption.path("lineId").asText();
            String description = disruption.path("description").asText();
            sb.append(lineId).append(": ").append(description).append("\n");
        }
        return sb.toString().trim();
    }

    private String fetchStopSearch(String query) throws Exception {
        String url = withAuth(tflBase + "/StopPoint/Search/" + query);
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());

        var sb = new StringBuilder();
        for (JsonNode match : root.path("matches")) {
            String id = match.path("id").asText();
            String name = match.path("name").asText();
            sb.append(id).append(" — ").append(name).append("\n");
        }
        return sb.toString().trim();
    }

    private String fetchArrivals(String stopId) throws Exception {
        String url = withAuth(tflBase + "/StopPoint/" + stopId + "/Arrivals");
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());

        var arrivals = new ArrayList<JsonNode>();
        for (JsonNode arrival : root) arrivals.add(arrival);
        arrivals.sort((a, b) -> Integer.compare(
                a.path("timeToStation").asInt(),
                b.path("timeToStation").asInt()));

        var sb = new StringBuilder();
        for (JsonNode arrival : arrivals) {
            String line = arrival.path("lineName").asText();
            String destination = arrival.path("destinationName").asText();
            int seconds = arrival.path("timeToStation").asInt();
            int minutes = seconds / 60;
            String platform = arrival.path("platformName").asText("");
            sb.append(line).append(" → ").append(destination)
              .append(": ").append(minutes).append(" min");
            if (!platform.isBlank()) sb.append(" (").append(platform).append(")");
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String fetchJourney(String from, String to) throws Exception {
        String url = withAuth(tflBase + "/Journey/JourneyResults/" + from + "/to/" + to);
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());

        var sb = new StringBuilder();
        int journeyNum = 1;
        for (JsonNode journey : root.path("journeys")) {
            int duration = journey.path("duration").asInt();
            sb.append("Journey ").append(journeyNum++).append(" (").append(duration).append(" min):\n");
            for (JsonNode leg : journey.path("legs")) {
                String summary = leg.path("instruction").path("summary").asText(
                        leg.path("summary").asText());
                int legDuration = leg.path("duration").asInt();
                sb.append("  - ").append(summary).append(" (").append(legDuration).append(" min)\n");
            }
        }
        return sb.toString().trim();
    }

    private String fetchBikePoints() throws Exception {
        String url = withAuth(tflBase + "/BikePoint");
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = JSON.readTree(response.body());

        var sb = new StringBuilder();
        for (JsonNode point : root) {
            String id = point.path("id").asText();
            String name = point.path("commonName").asText();
            String bikes = "";
            String emptyDocks = "";
            for (JsonNode prop : point.path("additionalProperties")) {
                String key = prop.path("key").asText();
                if ("NbBikes".equals(key)) bikes = prop.path("value").asText();
                else if ("NbEmptyDocks".equals(key)) emptyDocks = prop.path("value").asText();
            }
            sb.append(id).append(" — ").append(name)
              .append(": ").append(bikes).append(" bikes, ").append(emptyDocks).append(" empty docks\n");
        }
        return sb.toString().trim();
    }

    private String fetchLineStatus(String lines) throws Exception {
        String url = withAuth(tflBase + "/Line/" + lines + "/Status");
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
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);
        var app = new App(transportProvider, "https://api.tfl.gov.uk");
        log.info("TfL MCP server started (stdio transport)");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { app.stop(); } catch (Exception ignored) {}
        }));
        Thread.currentThread().join();
    }
}
