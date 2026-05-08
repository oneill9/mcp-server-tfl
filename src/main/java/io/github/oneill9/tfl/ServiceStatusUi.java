package io.github.oneill9.tfl;

import java.nio.charset.StandardCharsets;

final class ServiceStatusUi {
    private static final String RESOURCE_PATH = "/service-status.html";

    private ServiceStatusUi() {}

    static String render() {
        try (var in = ServiceStatusUi.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing shared resource: " + RESOURCE_PATH);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load service status UI resource", e);
        }
    }
}
