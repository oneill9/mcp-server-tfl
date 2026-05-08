package io.github.oneill9.tfl;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static final String TFL_APP_KEY = System.getenv("TFL_APP_KEY");
    private static final String TFL_APP_ID = System.getenv("TFL_APP_ID");
    private static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SERVICE_STATUS_UI_URI = "ui://tfl/service-status";
    private static final String MCP_APP_MIME_TYPE = "text/html;profile=mcp-app";
    private static final List<String> SERVICE_STATUS_MODES = List.of(
            "tube",
            "bus",
            "overground",
            "elizabeth-line",
            "dlr",
            "tube,bus",
            "tube,overground,elizabeth-line,dlr");

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
    private Server jettyServer;

    public App(McpServerTransportProvider transportProvider, String tflBase) {
        this.tflBase = tflBase;

        var arrivalsSchema = new McpSchema.JsonSchema(
                "object",
                Map.of("stopName", Map.of("type", "string", "description", "Stop name or search term, e.g. oxford")),
                List.of("stopName"),
                null, null, null);
        var serviceStatusSchema = serviceStatusSchema();
        var journeySchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "from", Map.of("type", "string", "description", "Origin: NaPTAN ID, postcode, or lat,lon"),
                        "to",   Map.of("type", "string", "description", "Destination: NaPTAN ID, postcode, or lat,lon")),
                List.of("from", "to"),
                null, null, null);
        var crowdingSchema = new McpSchema.JsonSchema(
                "object",
                Map.of("stopName", Map.of("type", "string", "description", "Stop name or search term, e.g. oxford")),
                List.of("stopName"),
                null, null, null);
        var faresSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "fromName", Map.of("type", "string", "description", "Origin stop name, e.g. oxford"),
                        "toName", Map.of("type", "string", "description", "Destination stop name, e.g. bank")),
                List.of("fromName", "toName"),
                null, null, null);
        var bikePointsSchema = new McpSchema.JsonSchema(
                "object",
                Map.of("query", Map.of("type", "string", "description", "Optional name search, e.g. clerkenwell")),
                List.of(),
                null, null, null);

        mcpServer = McpServer.sync(transportProvider)
                .serverInfo("TfL", VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .resources(false, false)
                        .build())
                .resources(new McpServerFeatures.SyncResourceSpecification(
                        McpSchema.Resource.builder()
                                .uri(SERVICE_STATUS_UI_URI)
                                .name("Service Status Board")
                                .description("Live London transport service status board")
                                .mimeType(MCP_APP_MIME_TYPE)
                                .build(),
                        (exchange, request) -> new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(
                                        SERVICE_STATUS_UI_URI,
                                        MCP_APP_MIME_TYPE,
                                        ServiceStatusUi.render())))))
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("arrivals")
                                .description("Get live arrivals at a TfL stop.")
                                .inputSchema(arrivalsSchema)
                                .annotations(new McpSchema.ToolAnnotations("Live Arrivals", true, false, true, true, null))
                                .build(),
                        (exchange, request) -> {
                            String stopName = request.arguments().get("stopName").toString();
                            try {
                                String stopId = resolveStopName(stopName);
                                String path = "/StopPoint/" + encodePath(stopId) + "/Arrivals";
                                logToolCall("arrivals", path, request.arguments(), arrivalsSchema, null);
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchArrivals(path))
                                        .build();
                            } catch (Exception e) {
                                logToolCall("arrivals", null, request.arguments(), arrivalsSchema, e.getMessage());
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching arrivals: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("service_status")
                                .description("Get the current operational status and delays for one or more TfL public transport modes.")
                                .inputSchema(serviceStatusSchema)
                                .meta(uiToolMeta(SERVICE_STATUS_UI_URI))
                                .annotations(new McpSchema.ToolAnnotations("Service Status", true, false, true, true, null))
                                .build(),
                        (exchange, request) -> {
                            String validationError = validateServiceStatusArguments(request.arguments());
                            if (validationError != null) {
                                logToolCall("service_status", null, request.arguments(), serviceStatusSchema, validationError);
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Invalid service_status arguments: " + validationError)
                                        .isError(true)
                                        .build();
                            }
                            String modes = request.arguments().get("modes").toString();
                            String path = serviceStatusPath(modes);
                            logToolCall("service_status", path, request.arguments(), serviceStatusSchema, null);
                            try {
                                JsonNode statuses = fetchServiceStatusData(path);
                                return serviceStatusResult(statuses);
                            } catch (Exception e) {
                                logToolCall("service_status", path, request.arguments(), serviceStatusSchema, e.getMessage());
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching service status: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("journey")
                                .description("Plan a journey between two points using the TfL Journey Planner. Can bridge different transport modes seamlessly.")
                                .inputSchema(journeySchema)
                                .annotations(new McpSchema.ToolAnnotations("Journey Planner", true, false, true, true, null))
                                .build(),
                        (exchange, request) -> {
                            String from = request.arguments().get("from").toString();
                            String to   = request.arguments().get("to").toString();
                            String path = "/Journey/JourneyResults/" + encodePath(from) + "/to/" + encodePath(to);
                            logToolCall("journey", path, request.arguments(), journeySchema, null);
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchJourney(path))
                                        .build();
                            } catch (Exception e) {
                                logToolCall("journey", path, request.arguments(), journeySchema, e.getMessage());
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching journey: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("crowding")
                                .description("Get live crowding data for a TfL station. Returns the current crowding level as a percentage of the typical baseline.")
                                .inputSchema(crowdingSchema)
                                .annotations(new McpSchema.ToolAnnotations("Station Crowding", true, false, true, true, null))
                                .build(),
                        (exchange, request) -> {
                            String stopName = request.arguments().get("stopName").toString();
                            try {
                                String naptan = resolveStopName(stopName);
                                String path = "/Crowding/" + encodePath(naptan) + "/Live";
                                logToolCall("crowding", path, request.arguments(), crowdingSchema, null);
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchCrowding(path, naptan))
                                        .build();
                            } catch (Exception e) {
                                logToolCall("crowding", null, request.arguments(), crowdingSchema, e.getMessage());
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching crowding data: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("fares")
                                .description("Get fare information between two TfL stops, including pay-as-you-go and cash single prices for peak and off-peak travel.")
                                .inputSchema(faresSchema)
                                .annotations(new McpSchema.ToolAnnotations("Fares", true, false, true, true, null))
                                .build(),
                        (exchange, request) -> {
                            String fromName = request.arguments().get("fromName").toString();
                            String toName = request.arguments().get("toName").toString();
                            try {
                                String fromStopId = resolveStopName(fromName);
                                String toStopId = resolveStopName(toName);
                                String path = "/StopPoint/" + encodePath(fromStopId) + "/FareTo/" + encodePath(toStopId);
                                logToolCall("fares", path, request.arguments(), faresSchema, null);
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchFares(path))
                                        .build();
                            } catch (Exception e) {
                                logToolCall("fares", null, request.arguments(), faresSchema, e.getMessage());
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching fares: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("bike_points")
                                .description("Get TfL Santander Cycles bike point locations with available bikes and empty docks. Optionally filter by name with a search query.")
                                .inputSchema(bikePointsSchema)
                                .annotations(new McpSchema.ToolAnnotations("Bike Points", true, false, true, true, null))
                                .build(),
                        (exchange, request) -> {
                            Object queryArg = request.arguments().get("query");
                            String query = queryArg != null ? queryArg.toString().trim() : "";
                            String path = query.isBlank() ? "/BikePoint" : "/BikePoint/Search/" + encodePath(query);
                            logToolCall("bike_points", path, request.arguments(), bikePointsSchema, null);
                            try {
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent(fetchBikePoints(path))
                                        .build();
                            } catch (Exception e) {
                                logToolCall("bike_points", path, request.arguments(), bikePointsSchema, e.getMessage());
                                return McpSchema.CallToolResult.builder()
                                        .addTextContent("Error fetching bike points: " + e.getMessage())
                                        .isError(true)
                                        .build();
                            }
                        })
                .build();
    }

    private static McpSchema.JsonSchema serviceStatusSchema() {
        var modes = new LinkedHashMap<String, Object>();
        modes.put("type", "string");
        modes.put("description", "Comma-separated TfL modes, e.g. tube,bus,overground,elizabeth-line,dlr");
        modes.put("enum", SERVICE_STATUS_MODES);
        return new McpSchema.JsonSchema(
                "object",
                Map.of("modes", modes),
                List.of("modes"),
                false, null, null);
    }

    private static Map<String, Object> uiToolMeta(String resourceUri) {
        return Map.of(
                "ui", Map.of("resourceUri", resourceUri),
                "ui/resourceUri", resourceUri);
    }

    private static void logToolCall(
            String toolName,
            String path,
            Map<String, Object> rawArgs,
            McpSchema.JsonSchema schema,
            String validationError) {
        try {
            log.info("Inbound MCP tool call: tool={} path={} rawArgs={} inputSchema={} validationError={}",
                    toolName,
                    path == null ? "<not resolved>" : path,
                    JSON.writeValueAsString(rawArgs),
                    JSON.writeValueAsString(schema),
                    validationError == null ? "<none>" : validationError);
        } catch (Exception e) {
            log.info("Inbound MCP tool call: tool={} path={} rawArgs={} validationError={}",
                    toolName,
                    path == null ? "<not resolved>" : path,
                    rawArgs,
                    validationError == null ? "<none>" : validationError);
        }
    }

    private static String validateServiceStatusArguments(Map<String, Object> arguments) {
        if (arguments == null) {
            return "arguments must be an object";
        }
        if (!arguments.keySet().equals(Set.of("modes"))) {
            return "expected exactly one property: modes";
        }
        Object value = arguments.get("modes");
        if (!(value instanceof String modes)) {
            return "modes must be a string";
        }
        if (modes.isBlank()) {
            return "modes must not be blank";
        }
        if (!SERVICE_STATUS_MODES.contains(modes)) {
            return "modes must be one of " + SERVICE_STATUS_MODES;
        }
        return null;
    }

    /**
     * Start the MCP server in HTTP mode with SSE transport on the given port.
     * Use port 0 for a dynamically allocated port (retrieve with {@link #getPort()}).
     */
    public static App startHttp(int port, String tflBase) throws Exception {
        var transportProvider = HttpServletSseServerTransportProvider.builder()
                .messageEndpoint("/mcp/message")
                .sseEndpoint("/sse")
                .build();

        var app = new App(transportProvider, tflBase);

        var jetty = new Server();
        var connector = new ServerConnector(jetty);
        connector.setPort(port);
        jetty.addConnector(connector);
        var context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(transportProvider), "/*");
        jetty.setHandler(context);
        jetty.start();

        app.jettyServer = jetty;
        return app;
    }

    /** Returns the HTTP port the server is listening on, or -1 if not in HTTP mode. */
    public int getPort() {
        if (jettyServer == null) return -1;
        return ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
    }

    public void stop() throws Exception {
        mcpServer.closeGracefully();
        if (jettyServer != null) jettyServer.stop();
    }

    private String withAuth(String url) {
        var params = new ArrayList<String>();
        if (TFL_APP_ID != null && !TFL_APP_ID.isBlank()) params.add("app_id=" + TFL_APP_ID);
        if (TFL_APP_KEY != null && !TFL_APP_KEY.isBlank()) params.add("app_key=" + TFL_APP_KEY);
        if (params.isEmpty()) return url;
        return url + "?" + String.join("&", params);
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodeSegments(String csv) {
        return Arrays.stream(csv.split(",", -1))
                .map(s -> URLEncoder.encode(s.trim(), StandardCharsets.UTF_8))
                .collect(Collectors.joining(","));
    }

    private JsonNode httpGet(String path) throws Exception {
        String url = withAuth(tflBase + path);
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new Exception("TfL API returned HTTP " + status);
        }
        return JSON.readTree(response.body());
    }

    private String resolveStopName(String query) throws Exception {
        JsonNode root = httpGet("/StopPoint/Search/" + encodePath(query));
        JsonNode matches = root.path("matches");
        if (matches.isMissingNode() || !matches.elements().hasNext()) {
            throw new Exception("No stop found for name: " + query);
        }
        return matches.elements().next().path("id").asText();
    }

    private String fetchArrivals(String path) throws Exception {
        JsonNode root = httpGet(path);
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

    private String fetchJourney(String path) throws Exception {
        JsonNode root = httpGet(path);
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


    private String fetchCrowding(String path, String naptan) throws Exception {
        JsonNode root = httpGet(path);
        boolean available = root.path("dataAvailable").asBoolean(false);
        if (!available) {
            return "Crowding data not available for " + naptan;
        }
        double pct = root.path("percentageOfBaseline").asDouble();
        long rounded = Math.round(pct * 100);
        return naptan + ": " + rounded + "% of typical crowding level (baseline ratio: " + pct + ")";
    }

    private String fetchFares(String path) throws Exception {
        JsonNode root = httpGet(path);
        var sb = new StringBuilder();
        for (JsonNode section : root) {
            for (JsonNode row : section.path("rows")) {
                String from = row.path("from").asText();
                String to = row.path("to").asText();
                String passenger = row.path("passengerType").asText("Adult");
                sb.append(from).append(" → ").append(to).append(" (").append(passenger).append("):\n");
                for (JsonNode ticket : row.path("ticketsAvailable")) {
                    String type = ticket.path("ticketType").asText();
                    String time = ticket.path("ticketTime").asText();
                    String cost = ticket.path("cost").asText();
                    sb.append("  £").append(cost).append(" — ").append(type);
                    if (!time.isBlank()) sb.append(" (").append(time).append(")");
                    sb.append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private String fetchBikePoints(String path) throws Exception {
        JsonNode root = httpGet(path);
        var sb = new StringBuilder();
        for (JsonNode point : root) {
            String id = point.path("id").asText();
            String name = point.path("commonName").asText();
            int bikes = 0;
            int emptyDocks = 0;
            for (JsonNode prop : point.path("additionalProperties")) {
                String key = prop.path("key").asText();
                String value = prop.path("value").asText("0");
                if ("NbBikes".equals(key)) bikes = Integer.parseInt(value);
                if ("NbEmptyDocks".equals(key)) emptyDocks = Integer.parseInt(value);
            }
            sb.append(name).append(" (").append(id).append("): ")
              .append(bikes).append(" bikes available, ")
              .append(emptyDocks).append(" empty docks\n");
        }
        return sb.toString().trim();
    }

    private static String serviceStatusPath(String modes) {
        return "/Line/Mode/" + encodeSegments(modes) + "/Status";
    }

    private JsonNode fetchServiceStatusData(String path) throws Exception {
        return httpGet(path);
    }

    private McpSchema.CallToolResult serviceStatusResult(JsonNode statuses) throws Exception {
        return McpSchema.CallToolResult.builder()
                .addTextContent(formatServiceStatus(statuses))
                .addContent(new McpSchema.EmbeddedResource(
                        null,
                        new McpSchema.TextResourceContents(
                                SERVICE_STATUS_UI_URI,
                                "application/json",
                                JSON.writeValueAsString(parseLineStatuses(statuses)))))
                .build();
    }

    private List<Map<String, Object>> parseLineStatuses(JsonNode root) {
        var lines = new ArrayList<Map<String, Object>>();
        for (JsonNode line : root) {
            var statuses = new ArrayList<Map<String, Object>>();
            for (JsonNode status : line.path("lineStatuses")) {
                var item = new java.util.LinkedHashMap<String, Object>();
                item.put("severity", status.path("statusSeverityDescription").asText());
                String reason = status.path("reason").asText("");
                if (!reason.isBlank()) item.put("reason", reason);
                statuses.add(item);
            }

            var item = new java.util.LinkedHashMap<String, Object>();
            item.put("id", line.path("id").asText());
            item.put("name", line.path("name").asText());
            item.put("statuses", statuses);
            lines.add(item);
        }
        return lines;
    }

    private String formatServiceStatus(JsonNode root) {
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
        boolean httpMode = Arrays.asList(args).contains("--http");

        final App app;
        if (httpMode) {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            app = startHttp(port, "https://api.tfl.gov.uk");
            log.info("TfL MCP server started (HTTP/SSE transport on port {})", app.getPort());
        } else {
            McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());
            var transportProvider = new StdioServerTransportProvider(jsonMapper);
            app = new App(transportProvider, "https://api.tfl.gov.uk");
            log.info("TfL MCP server started (stdio transport)");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { app.stop(); } catch (Exception ignored) {}
        }));
        Thread.currentThread().join();
    }
}
