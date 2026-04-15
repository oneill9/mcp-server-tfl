package io.github.oneill9.tfl;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests via stdio transport — the same mechanism LLM clients (e.g.
 * Claude Desktop) use to connect to MCP servers. Spawns the App as a real
 * subprocess and communicates over stdin/stdout.
 *
 * TFL_APP_KEY is deliberately excluded from the subprocess environment to
 * verify the server works correctly without authentication.
 */
@Tag("contract")
class StdioContractTest {

    static McpSyncClient client;
    static StdioClientTransport transport;

    @BeforeAll
    static void setUp() throws Exception {
        // Resolve the java binary from the currently-running JVM so we use
        // the same JDK (and avoid PATH ambiguity in CI).
        String javaCmd = ProcessHandle.current().info().command().orElse("java");

        // The test JVM's classpath already contains App and all its deps,
        // so we can reuse it directly for the subprocess.
        String classpath = System.getProperty("java.class.path");

        // Explicitly omit TFL_APP_KEY / TFL_APP_ID to exercise the unauthenticated
        // code path — TfL allows anonymous low-volume access.
        var env = new HashMap<>(System.getenv());
        env.remove("TFL_APP_KEY");
        env.remove("TFL_APP_ID");

        var params = ServerParameters.builder(javaCmd)
                .args("-cp", classpath, "io.github.oneill9.tfl.App")
                .env(env)
                .build();

        var jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());
        transport = new StdioClientTransport(params, jsonMapper);
        // Silence the server's stderr logs in test output
        transport.setStdErrorHandler(line -> {});

        client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();

        client.initialize();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (client != null) client.close();
    }

    // --- connectivity ---

    @Test
    void serverReportsCorrectInfo() {
        var info = client.getServerInfo();
        assertEquals("TfL", info.name());
    }

    @Test
    void serverAdvertisesAllTools() {
        var tools = client.listTools().tools();
        var names = tools.stream().map(McpSchema.Tool::name).toList();
        assertTrue(names.contains("service_status"));
        assertTrue(names.contains("arrivals"));
        assertTrue(names.contains("journey"));
        assertTrue(names.contains("bike_points"));
        assertTrue(names.contains("crowding"));
        assertTrue(names.contains("fares"));
    }

    // --- tools (unauthenticated) ---

    @Test
    void serviceStatusWithoutKey() {
        var result = client.callTool(new McpSchema.CallToolRequest("service_status",
                Map.of("modes", "tube")));
        assertFalse(result.isError(), "service_status should succeed without an API key");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.toLowerCase().contains("central"), "Response should mention Central line");
    }

    @Test
    void bikePointsWithoutKey() {
        var result = client.callTool(new McpSchema.CallToolRequest("bike_points", Map.of()));
        assertFalse(result.isError(), "bike_points should succeed without an API key");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("BikePoints_"), "Response should include station IDs");
    }

    @Test
    void arrivalsWithoutKey() {
        var result = client.callTool(new McpSchema.CallToolRequest("arrivals",
                Map.of("stopName", "Oxford Circus Underground Station")));
        assertFalse(result.isError(), "arrivals should succeed without an API key");
    }

    @Test
    void journeyWithoutKey() {
        var result = client.callTool(new McpSchema.CallToolRequest("journey", Map.of(
                "from", "51.4952,-0.1441",
                "to",   "51.5179,-0.0816")));
        assertFalse(result.isError(), "journey should succeed without an API key");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "journey should return a non-empty response");
    }

    @Test
    void crowdingWithoutKey() {
        var result = client.callTool(new McpSchema.CallToolRequest("crowding",
                Map.of("stopName", "Oxford Circus Underground Station")));
        assertFalse(result.isError(), "crowding should succeed without an API key");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "Response should be non-empty");
    }

    @Test
    void faresWithoutKey() {
        var result = client.callTool(new McpSchema.CallToolRequest("fares",
                Map.of("fromName", "Oxford Circus Underground Station", "toName", "Bond Street Underground Station")));
        assertFalse(result.isError(), "fares should succeed without an API key");
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertFalse(text.isBlank(), "Response should be non-empty");
    }
}
