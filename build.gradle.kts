plugins {
    java
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.mcp.sdk)

    // Jetty 12 as servlet container
    implementation(libs.jetty.server)
    implementation(libs.jetty.servlet)

    // JSON parsing for TfL API responses
    implementation(libs.jackson.databind)

    // Logging
    implementation(libs.slf4j.api)
    runtimeOnly(libs.log4j.slf4j2.impl)
    runtimeOnly(libs.log4j.core)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "com.aon.tfl.App"
}
