# 建置與執行

## 建置

切換至 `bestpartner-service` 目錄，執行：

```bash
./gradlew clean build -x test \
  -Dquarkus.package.type=uber-jar \
  -Dorg.gradle.daemon=false \
  -Dquarkus.profile=${profile}
```

`${profile}` 可選值：`dev`（預設）、`sit`、`prod`

產生的 jar：`bestpartner-service/build/bestpartner-service-0.1.7-SNAPSHOT-runner.jar`

## 執行

```bash
java -jar bestpartner-service-0.1.7-SNAPSHOT-runner.jar
```

服務預設埠：**port 80**
