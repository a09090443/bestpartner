# Gradle 建置規範

## 版本管理

所有依賴版本必須在 `gradle.properties` 中統一定義，**禁止在 `build.gradle.kts` 中硬編碼版本號**。

### gradle.properties 版本定義

```properties
# Quarkus BOM 版本（來自 Quarkus 官方）
quarkusPluginVersion=3.21.0
quarkusPlatformVersion=3.21.0

# 第三方庫版本
kotlinVersion=2.1.0
langchain4jVersion=1.12.2
bouncycastleVersion=1.79
okhttp3Version=4.12.0
kotlinSerializationVersion=1.7.3
kotlinxCoroutinesVersion=1.9.1
```

### build.gradle.kts 版本讀取

```kotlin
// 在檔案開始處宣告版本變數（使用 by project 讀取 gradle.properties）
val kotlinVersion: String by project
val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val langchain4jVersion: String by project
val bouncycastleVersion: String by project
val okhttp3Version: String by project
val kotlinSerializationVersion: String by project
val kotlinxCoroutinesVersion: String by project

// 在 dependencies 中使用變數
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("dev.langchain4j:langchain4j:$langchain4jVersion")
    // ...
}
```

### 特例：Plugin 版本

Plugin 版本必須直接在 `plugins` 區塊中指定，**無法使用 `by project` 讀取**：

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    kotlin("plugin.allopen") version "2.1.0"
    id("io.quarkus")
}
```

## 版本更新流程

新增或更新依賴時：

1. 在 `gradle.properties` 中新增或修改版本變數
2. 在 `build.gradle.kts` 中用 `$variableName` 參考該版本
3. 確保 `build.gradle.kts` 中**不存在硬編碼版本號**（plugins 區塊除外）
