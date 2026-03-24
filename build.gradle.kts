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
    implementation(libs.mcp.sdk)

    // Jackson 3 (tools.jackson) — required by StdioServerTransportProvider / JacksonMcpJsonMapper
    implementation(libs.jackson3.databind)

    // JSON parsing for TfL API responses (Jackson 2)
    implementation(libs.jackson.databind)

    // Logging
    implementation(libs.slf4j.api)
    runtimeOnly(libs.log4j.slf4j2.impl)
    runtimeOnly(libs.log4j.core)

    // Testing — Jetty used only to host the SSE transport in tests
    testImplementation(libs.jetty.server)
    testImplementation(libs.jetty.servlet)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.processResources {
    val versionValue = providers.gradleProperty("version")
    filesMatching("version.properties") {
        filter { line -> line.replace("\${version}", versionValue.get()) }
    }
}

val contractTestSourceSet = sourceSets.create("contractTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[contractTestSourceSet.implementationConfigurationName]
        .extendsFrom(configurations.testImplementation.get())
configurations[contractTestSourceSet.runtimeOnlyConfigurationName]
        .extendsFrom(configurations.testRuntimeOnly.get())

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("contractTest") {
    description = "Runs contract tests against the real TfL API."
    group = "verification"
    testClassesDirs = contractTestSourceSet.output.classesDirs
    classpath = contractTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

application {
    mainClass = "com.aon.tfl.App"
}
