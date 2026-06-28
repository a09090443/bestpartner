<#
.SYNOPSIS
    BestPartner Harness 漂移掃描（Harness 原則 6：Entropy Management）。

.DESCRIPTION
    可重複執行的程序，逐項對照 .claude/rules/ 的黃金規範，掃描跨模組慣例漂移。
    輸出「現況 vs 規範 vs 建議」表，並區分「歷史基線」與「本次新漂移」。

    - 結構性不變量（分層依賴、命名、@Entity 位置）由 ArchUnit 在 build 階段強制，
      本掃描補足 ArchUnit 不易檢查的慣例（i18n 寫死字串、版本硬編碼、套件拼字、CDI 標註）。
    - 歷史基線（既存且刻意容忍的漂移）預先登錄於下方 $Baseline，不視為失敗。
    - 偵測到「新漂移」時以 exit code 1 結束，可作為 CI 把關。

.EXAMPLE
    pwsh ./harness-drift-scan.ps1
#>

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$srcMain = Join-Path $root 'bestpartner-service/src/main/kotlin/tw/zipe/bastpartner'
$buildGradle = Join-Path $root 'bestpartner-service/build.gradle.kts'

# ---- 歷史基線（既存且刻意容忍的漂移；新增程式碼不得再擴大）----
$Baseline = @{
    # @Entity 但無 Entity 後綴（嵌入式 *Id 類為 @Embeddable，另行排除）
    EntityNoSuffix      = @('LLMMcpUserSetting')
    # 仍以字面字串拋例外、尚未改用 AppMessage 的檔案
    I18nHardcodeFiles   = @('LLMStore.kt', 'LLMResource.kt')
    # build.gradle.kts 仍硬編碼版本的依賴 artifact
    VersionHardcodeArts = @('jsqlparser')
}

$findings = New-Object System.Collections.Generic.List[object]
function Add-Finding($Rule, $Status, $Detail, $Advice) {
    $findings.Add([pscustomobject]@{ Rule = $Rule; Status = $Status; Detail = $Detail; Advice = $Advice })
}

$ktFiles = Get-ChildItem $srcMain -Recurse -Filter *.kt

# 規則 1：套件拼字必須是 bastpartner（非 basepartner）
$typo = $ktFiles | Select-String -Pattern 'tw\.zipe\.basepartner' -List
if ($typo) {
    foreach ($t in $typo) { Add-Finding '套件拼字' 'NEW' "$($t.Filename):$($t.LineNumber)" '改為 tw.zipe.bastpartner（少一個 e）' }
} else {
    Add-Finding '套件拼字' 'OK' '0 處誤用 basepartner' '—'
}

# 規則 2：業務訊息不得寫死字串（應用 AppMessage + i18n）
$hardExc = $ktFiles | Select-String -Pattern '(ServiceException|LLMException)\("'
$hardByFile = $hardExc | Group-Object Filename
foreach ($g in $hardByFile) {
    if ($Baseline.I18nHardcodeFiles -contains $g.Name) {
        Add-Finding 'i18n 寫死字串' 'BASELINE' "$($g.Name)（$($g.Count) 處）" '排程改用 AppMessage（i18n-messages.md）'
    } else {
        Add-Finding 'i18n 寫死字串' 'NEW' "$($g.Name)（$($g.Count) 處）" '改用 AppMessage enum，勿寫死字串'
    }
}
if (-not $hardByFile) { Add-Finding 'i18n 寫死字串' 'OK' '無新增寫死字串' '—' }

# 規則 3：build.gradle.kts 版本不得硬編碼（plugins 區塊除外）
$verHard = Select-String -Path $buildGradle -Pattern 'implementation\("([^"]*):[0-9]+\.[0-9]+'
foreach ($v in $verHard) {
    $art = ($v.Matches[0].Groups[1].Value -split ':')[-1]
    if ($Baseline.VersionHardcodeArts -contains $art) {
        Add-Finding '版本硬編碼' 'BASELINE' "$art（行 $($v.LineNumber)）" '排程移版本至 gradle.properties'
    } else {
        Add-Finding '版本硬編碼' 'NEW' "$art（行 $($v.LineNumber)）" '版本移至 gradle.properties，以 $xxxVersion 引用'
    }
}
if (-not $verHard) { Add-Finding '版本硬編碼' 'OK' '無硬編碼版本' '—' }

# 規則 4：@Entity 類別命名應以 Entity 結尾（嵌入式 *Id 排除）
$entityDir = Join-Path $srcMain 'entity'
$entityFiles = Get-ChildItem $entityDir -Filter *.kt
foreach ($f in $entityFiles) {
    $name = $f.BaseName
    $isEntity = Select-String -Path $f.FullName -Pattern '@Entity' -Quiet
    if ($isEntity -and ($name -notlike '*Entity') -and ($name -notlike '*Id')) {
        if ($Baseline.EntityNoSuffix -contains $name) {
            Add-Finding 'Entity 命名' 'BASELINE' $name '排程重新命名為 XxxEntity（破壞性，另行評估）'
        } else {
            Add-Finding 'Entity 命名' 'NEW' $name '@Entity 類別請命名為 XxxEntity.kt'
        }
    }
}

# 規則 5：service / repository 套件類別必須標 @ApplicationScoped（Base 類除外）
foreach ($pkg in @('service', 'repository')) {
    $pkgDir = Join-Path $srcMain $pkg
    foreach ($f in (Get-ChildItem $pkgDir -Filter *.kt)) {
        if ($f.BaseName -like 'Base*') { continue }
        if (-not (Select-String -Path $f.FullName -Pattern '@ApplicationScoped' -Quiet)) {
            Add-Finding "CDI 標註($pkg)" 'NEW' $f.BaseName '加上 @ApplicationScoped'
        }
    }
}
if (-not ($findings | Where-Object Rule -like 'CDI*')) {
    Add-Finding 'CDI 標註' 'OK' 'service/repository 皆已標註' '—'
}

# ---- 輸出 ----
Write-Host ''
Write-Host '=== BestPartner Harness 漂移掃描 ===' -ForegroundColor Cyan
$findings | Sort-Object Rule | Format-Table -AutoSize -Wrap Rule,
    @{ L = 'Status'; E = {
        switch ($_.Status) { 'NEW' { "⚠ NEW" } 'BASELINE' { 'BASELINE' } default { '✓ OK' } } } },
    Detail, Advice | Out-Host

$new = @($findings | Where-Object Status -eq 'NEW')
$base = @($findings | Where-Object Status -eq 'BASELINE')
Write-Host ("基線 {0} 項、新漂移 {1} 項。" -f $base.Count, $new.Count) -ForegroundColor Yellow
Write-Host '提醒：結構性不變量（分層、命名、@Entity 位置）另由 ArchitectureTest.kt 強制：' -ForegroundColor DarkGray
Write-Host '  cd bestpartner-service; ./gradlew test --tests "tw.zipe.bastpartner.architecture.ArchitectureTest"' -ForegroundColor DarkGray

if ($new.Count -gt 0) {
    Write-Host "`n發現 $($new.Count) 項新漂移，請修正或（確認可接受後）登錄為基線。" -ForegroundColor Red
    exit 1
}
Write-Host "`n無新漂移。" -ForegroundColor Green
exit 0
