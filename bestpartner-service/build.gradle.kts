import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    kotlin("plugin.allopen") version "2.1.0"
    id("io.quarkus")
}

repositories {
    maven {
        url = uri("https://repo1.maven.org/maven2/")
    }
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}

val kotlinVersion: String by project
val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val langchain4jVersion: String by project
val bouncycastleVersion: String by project
val okhttp3Version: String by project
val kotlinSerializationVersion: String by project
val kotlinxCoroutinesVersion: String by project
val archunitVersion: String by project
val graalJsVersion: String by project

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
        // Quarkus BOM 管的 Truffle 為 23.1.x，與 GraalJS 24.x 不相容；polyglot 與 truffle 版本必須一致
        eachDependency {
            if (requested.group.startsWith("org.graalvm.")) {
                useVersion(graalJsVersion)
                because("GraalJS sandbox 需 polyglot/truffle 版本對齊 $graalJsVersion")
            }
        }
    }
}

// GraalJS 的 truffle-api 為 multi-release jar 且含 META-INF/resources/engine/libtruffleattach/**
// （Truffle native attach 函式庫，JVM interpreter 模式非必要）。Quarkus 3.21 的
// StaticResourcesProcessor 掃描 web 靜態資源時，對「Multi-Release + META-INF/resources 並存」
// 的 jar 計算 multi-release 版本路徑相對位移時 substring 越界（StringIndexOutOfBoundsException），
// 導致 uber-jar 建置失敗。以 artifact transform 剝除此類 jar 的 META-INF/resources，
// 使掃描不進 multi-release 分支即可避開。命中條件精準（同時具兩者），實務上僅 truffle-api。
val strippedStaticResources = Attribute.of("stripped-static-resources", Boolean::class.javaObjectType)

dependencies {
    attributesSchema.attribute(strippedStaticResources)
    artifactTypes.getByName("jar").attributes.attribute(strippedStaticResources, false)
    registerTransform(StripMultiReleaseStaticResources::class) {
        from.attribute(strippedStaticResources, false)
            .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
        to.attribute(strippedStaticResources, true)
            .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
    }
}

configurations.all {
    if (isCanBeResolved) {
        attributes.attribute(strippedStaticResources, true)
    }
}

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation(enforcedPlatform("dev.langchain4j:langchain4j-bom:${langchain4jVersion}"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-kotlin")
    implementation("io.quarkus:quarkus-container-image-docker")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-smallrye-context-propagation")
    implementation("io.quarkus:quarkus-websockets-next")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-rest-kotlin-serialization")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-jdbc-mysql")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-security-jpa")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-security")
    implementation("io.quarkus:quarkus-smallrye-openapi")

    implementation("dev.langchain4j:langchain4j")
    implementation("dev.langchain4j:langchain4j-core")
    implementation("dev.langchain4j:langchain4j-ollama")
    implementation("dev.langchain4j:langchain4j-open-ai")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini")
    implementation("dev.langchain4j:langchain4j-anthropic")
    implementation("dev.langchain4j:langchain4j-chroma")
    implementation("dev.langchain4j:langchain4j-milvus")
    implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox")
    implementation("dev.langchain4j:langchain4j-document-parser-apache-tika")
    implementation("dev.langchain4j:langchain4j-embeddings-bge-small-en-v15-q")
    implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom")
    implementation("dev.langchain4j:langchain4j-web-search-engine-tavily")
    implementation("dev.langchain4j:langchain4j-mcp")
    implementation("dev.langchain4j:langchain4j-skills")

    implementation("com.github.jsqlparser:jsqlparser:5.1")

    implementation("org.bouncycastle:bcprov-jdk18on:$bouncycastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttp3Version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

    // GraalJS sandbox（CODE 節點；js-community 為 pom 型別聚合依賴，Gradle 以 module metadata 解析）
    implementation("org.graalvm.polyglot:polyglot:$graalJsVersion")
    implementation("org.graalvm.polyglot:js-community:$graalJsVersion")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
}

group = "tw.zipe.basepartner"
version = "0.1.8-SNAPSHOT"

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
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        javaParameters = true
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("compileQuarkusGeneratedSourcesJava"))
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

/**
 * 剝除「同時具 Multi-Release manifest 與 META-INF/resources 目錄」之 jar 的 META-INF/resources，
 * 其餘 jar 原樣通過。用於避開 Quarkus StaticResourcesProcessor 對此類 jar 的 multi-release
 * 路徑掃描越界（見上方 strippedStaticResources 說明）。命中者實務上僅 GraalJS 的 truffle-api。
 */
abstract class StripMultiReleaseStaticResources : TransformAction<TransformParameters.None> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (!input.name.endsWith(".jar", ignoreCase = true) || !input.isFile) {
            outputs.file(inputArtifact)
            return
        }

        val (multiRelease, hasResources) = ZipFile(input).use { zip ->
            val mr = zip.getEntry("META-INF/MANIFEST.MF")?.let { entry ->
                zip.getInputStream(entry).bufferedReader().useLines { lines ->
                    lines.any { it.trim().equals("Multi-Release: true", ignoreCase = true) }
                }
            } ?: false
            val hr = zip.entries().asSequence().any { it.name.startsWith("META-INF/resources/") }
            mr to hr
        }

        if (!(multiRelease && hasResources)) {
            outputs.file(inputArtifact)
            return
        }

        val output = outputs.file(input.nameWithoutExtension + "-stripped-resources.jar")
        ZipFile(input).use { zip ->
            ZipOutputStream(output.outputStream().buffered()).use { out ->
                for (entry in zip.entries()) {
                    if (entry.name.startsWith("META-INF/resources/")) continue
                    out.putNextEntry(ZipEntry(entry.name))
                    if (!entry.isDirectory) {
                        zip.getInputStream(entry).use { it.copyTo(out) }
                    }
                    out.closeEntry()
                }
            }
        }
    }
}
