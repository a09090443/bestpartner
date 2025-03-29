plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.allopen") version "2.0.21"
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val mcpServerStdioVersion = "1.0.0.CR1"
val googleApiClientVersion = "2.7.2"
val googleOauthClientJettyVersion = "1.39.0"
val googleApiServicesDriveVersion = "v3-rev20250220-2.0.0"

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkiverse.mcp:quarkus-mcp-server-stdio:$mcpServerStdioVersion")

    implementation("com.google.api-client:google-api-client:$googleApiClientVersion")
    implementation("com.google.oauth-client:google-oauth-client-jetty:$googleOauthClientJettyVersion")
    implementation("com.google.apis:google-api-services-drive:$googleApiServicesDriveVersion")
    implementation("commons-io:commons-io:2.18.0")

    testImplementation("io.quarkus:quarkus-junit5")
}

group = "com.example"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        javaParameters = true
    }
}
