plugins {
    java
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:1.1.0")

    // Jetty 12 as servlet container
    implementation("org.eclipse.jetty:jetty-server:12.0.18")
    implementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:12.0.18")

    // JSON parsing for TfL API responses
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testImplementation("org.wiremock:wiremock-jetty12:3.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "com.aon.tfl.App"
}
