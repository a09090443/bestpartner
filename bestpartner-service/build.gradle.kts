import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    kotlin("plugin.allopen") version "2.1.0"
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val langchain4jVersion = "0.36.2"
val bouncycastleVersion = "1.79"
val okhttp3Version = "4.12.0"
val kotlinSerializationVersion = "1.7.3"

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-kotlin")
    implementation("io.quarkus:quarkus-container-image-docker")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-websockets-next")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-rest-kotlin-serialization")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-jdbc-mysql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-security-jpa")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")

    implementation("dev.langchain4j:langchain4j:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-core:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-chroma:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-milvus:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-document-parser-apache-tika:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-embeddings-bge-small-en-v15-q:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-web-search-engine-tavily:$langchain4jVersion")

    implementation("me.kpavlov.langchain4j.kotlin:langchain4j-kotlin:0.1.4")
    implementation("com.github.jsqlparser:jsqlparser:5.1")

    implementation("org.bouncycastle:bcprov-jdk18on:$bouncycastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttp3Version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion") // Add this line

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation("io.rest-assured:rest-assured")
}

group = "tw.zipe.basepartner"
version = "0.1.4-SNAPSHOT"

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget("21")) // 使用 JvmTarget.fromTarget 轉換字串
        javaParameters.set(true) // 啟用參數名稱保留
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("compileQuarkusGeneratedSourcesJava"))
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
