#!/usr/bin/env pwsh
<#
.SYNOPSIS
    將 bestpartner-service 打包為跨環境 Docker image，驗證可啟動，並匯出 tar。

.DESCRIPTION
    產出「一份 image 跑所有環境」的成品。環境差異不在打包階段決定，
    而是在 docker run 時由 -e QUARKUS_PROFILE 與 --env-file 指定，
    因此本腳本沒有、也不該有「環境」參數。

    流程：讀版本 → 建 uber-jar(prod) → docker build → 啟動驗證 → 清理 → 匯出 tar

    建置 profile 固定為 prod：建置時的 profile 會成為 image 的預設 runtime
    profile，用 prod 才能讓部署時漏帶 QUARKUS_PROFILE 落在「Swagger 關閉、
    log INFO」的安全側。

    跨平台：以 PowerShell Core (pwsh) 撰寫，Windows / Linux / macOS 皆可執行，
    與 repo 既有的 harness-drift-scan.ps1 同一套慣例（CI 亦以 pwsh 於 ubuntu 執行）。

.PARAMETER VerifyPort
    啟動驗證用的宿主機埠，預設 18080。若該埠被占用可改用其他埠。

.PARAMETER SkipVerify
    跳過啟動驗證。不建議使用——略過即無法保證產出的 image 跑得起來。

.PARAMETER NoTar
    只建 image，不匯出 tar。適用於本機測試、不需搬遷的情況。

.EXAMPLE
    pwsh ./docker-package.ps1
    完整流程：建置 → 驗證 → 匯出 tar

.EXAMPLE
    pwsh ./docker-package.ps1 -NoTar
    只建 image 供本機測試

.EXAMPLE
    # 產出後依環境部署（同一份 image）
    docker run -d -p 80:80 -e QUARKUS_PROFILE=uat --env-file .env.uat `
      -v bestpartner-data:/opt/bestpartner bestpartner-service:latest
#>

[CmdletBinding()]
param(
    [int]$VerifyPort = 18080,
    [switch]$SkipVerify,
    [switch]$NoTar
)

$ErrorActionPreference = 'Stop'

$root       = $PSScriptRoot
$serviceDir = Join-Path $root 'bestpartner-service'
$dockerfile = 'src/main/docker/Dockerfile.uber-jar'
$verifyName = 'bestpartner-verify'
$imageName  = 'bestpartner-service'

function Write-Step { param($n, $msg) Write-Host "`n[$n] $msg" -ForegroundColor Cyan }
function Write-Ok   { param($msg) Write-Host "    ✓ $msg" -ForegroundColor Green }
function Write-Fail { param($msg) Write-Host "    ✗ $msg" -ForegroundColor Red }

Write-Host "=== BestPartner Docker 打包 ===" -ForegroundColor White

# --- 前置檢查 ------------------------------------------------------------
Write-Step 0 '前置檢查'

if (-not (Test-Path $serviceDir)) {
    throw "找不到 bestpartner-service 目錄：$serviceDir"
}

$dockerfilePath = Join-Path $serviceDir $dockerfile
if (-not (Test-Path $dockerfilePath)) {
    throw @"
找不到 $dockerfile。
注意：src/main/docker 下其餘 Dockerfile 為 Quarkus 預設產生，對應
fast-jar / legacy-jar / native 佈局，與本專案 uber-jar 建置不符，不可替代。
"@
}
Write-Ok "Dockerfile.uber-jar 存在"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw '找不到 docker 指令，請先安裝 Docker 並確認已加入 PATH。'
}
try { docker info 2>&1 | Out-Null } catch { }
if ($LASTEXITCODE -ne 0) {
    throw 'Docker daemon 未執行，請先啟動 Docker Desktop / dockerd。'
}
Write-Ok 'Docker daemon 可用'

# --- 讀版本（不硬編碼） ---------------------------------------------------
Write-Step 1 '讀取版本號'

$buildGradle = Join-Path $serviceDir 'build.gradle.kts'
$versionLine = Select-String -Path $buildGradle -Pattern '^version\s*=\s*"([^"]+)"' | Select-Object -First 1
if (-not $versionLine) {
    throw "無法從 $buildGradle 解析 version，請確認該檔含 version = `"x.y.z-SNAPSHOT`" 格式。"
}
$version = $versionLine.Matches[0].Groups[1].Value
Write-Ok "版本：$version"

# --- 建 uber-jar ---------------------------------------------------------
Write-Step 2 '建置 uber-jar（profile 固定 prod）'

Push-Location $serviceDir
try {
    # gradlew 在 Windows 需用 .bat；POSIX 用 shell script
    $gradlew = if ($IsWindows) { '.\gradlew.bat' } else { './gradlew' }

    # -D 參數必須加引號：PowerShell 的參數剖析會把 -Dquarkus.package.type=uber-jar
    # 拆開，導致 Gradle 收到 '.package.type=uber-jar' 並當成 task 名稱而失敗。
    & $gradlew clean build -x test `
        '-Dquarkus.package.type=uber-jar' `
        '-Dorg.gradle.daemon=false' `
        '-Dquarkus.profile=prod'
    if ($LASTEXITCODE -ne 0) {
        throw @'
Gradle 建置失敗。
若錯誤是 Unable to delete directory 'build'，通常為 IDE 或防毒鎖住 jar，
重跑一次多半即可解決。
'@
    }

    $jar = Join-Path $serviceDir "build/$imageName-$version-runner.jar"
    if (-not (Test-Path $jar)) { throw "建置宣稱成功，但找不到產物：$jar" }
    $jarMb = [math]::Round((Get-Item $jar).Length / 1MB)
    Write-Ok "uber-jar 產出（$jarMb MB）"

    # --- docker build ----------------------------------------------------
    Write-Step 3 'docker build'

    docker build -f $dockerfile -t "${imageName}:$version" -t "${imageName}:latest" .
    if ($LASTEXITCODE -ne 0) { throw 'docker build 失敗' }
    Write-Ok "image 標記為 ${imageName}:$version 與 ${imageName}:latest"

    # --- 啟動驗證 --------------------------------------------------------
    if ($SkipVerify) {
        Write-Step 4 '啟動驗證（已依 -SkipVerify 跳過）'
        Write-Host '    ⚠ 未驗證的 image 不保證跑得起來' -ForegroundColor Yellow
    }
    else {
        Write-Step 4 "啟動驗證（宿主埠 $VerifyPort）"

        docker rm -f $verifyName 2>&1 | Out-Null
        try {
            docker run -d --name $verifyName -p "${VerifyPort}:80" "${imageName}:latest" | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "容器啟動失敗（埠 $VerifyPort 可能被占用，可用 -VerifyPort 指定其他埠）" }

            # 存活探測用 /view/chat：公開且不受 profile 影響。
            # 不可用 /q/openapi 或 /swagger-ui——預設 prod 下它們回 404 是正確行為。
            $ok = $false
            foreach ($i in 1..30) {
                Start-Sleep -Seconds 1
                try {
                    $resp = Invoke-WebRequest -Uri "http://localhost:$VerifyPort/view/chat" `
                        -TimeoutSec 3 -SkipHttpErrorCheck -ErrorAction Stop
                    if ($resp.StatusCode -eq 200) { $ok = $true; break }
                }
                catch { }   # 服務尚未就緒，續試
            }

            if (-not $ok) {
                Write-Fail '/view/chat 未回 200，容器 log 如下：'
                docker logs $verifyName 2>&1 | Select-Object -Last 25
                throw '啟動驗證失敗'
            }

            $profile = (docker logs $verifyName 2>&1 | Select-String -Pattern 'Profile (\w+) activated' |
                        Select-Object -First 1).Matches.Groups[1].Value
            Write-Ok "/view/chat 回 200（預設 profile：$profile）"
        }
        finally {
            # 無論成敗都不留容器
            docker rm -f $verifyName 2>&1 | Out-Null
            Write-Ok '暫存容器已清理'
        }
    }

    # --- 匯出 tar --------------------------------------------------------
    if ($NoTar) {
        Write-Step 5 '匯出 tar（已依 -NoTar 跳過）'
        $tarPath = $null
    }
    else {
        Write-Step 5 '匯出 tar'

        Get-ChildItem -Path 'build' -Filter '*.tar' -ErrorAction SilentlyContinue | Remove-Item -Force
        $stamp   = Get-Date -Format 'yyyyMMddHHmm'
        $tarName = "$imageName-$version-prod-$stamp.tar"
        $tarPath = Join-Path $serviceDir "build/$tarName"

        docker save -o "build/$tarName" "${imageName}:latest"
        if ($LASTEXITCODE -ne 0) { throw 'docker save 失敗' }

        $tarMb = [math]::Round((Get-Item $tarPath).Length / 1MB)
        Write-Ok "$tarName（$tarMb MB）"
    }
}
finally {
    Pop-Location
}

# --- 回報 ----------------------------------------------------------------
Write-Host "`n=== 完成 ===" -ForegroundColor White
Write-Host "image : ${imageName}:$version、${imageName}:latest"
if ($tarPath) {
    Write-Host "tar   : $tarPath"
    Write-Host ''
    Write-Host '⚠ 此 tar 內含 JWT 簽章私鑰（privateKey.pem 隨 uber-jar 打包），' -ForegroundColor Yellow
    Write-Host '  等同憑證，不可在不受控管道流通。' -ForegroundColor Yellow
}

Write-Host @"

檔名的 -prod 是「建置 profile」，不是部署目標。
這一份 image 全環境通用，跑哪個環境由 runtime 決定：

  docker run -d -p 80:80 -e QUARKUS_PROFILE=uat --env-file .env.uat ``
    -v bestpartner-data:/opt/bestpartner ${imageName}:latest

  profile      Swagger/OpenAPI   SQL 日誌   log
  (未指定)     關                關         INFO   ← 安全預設
  dev/docker/sit  開             開         DEBUG
  uat/prod     關                關         INFO

存活探測請用 /view/chat；/swagger-ui 與 /q/openapi 在 uat/prod 回 404 為正確行為。
"@
