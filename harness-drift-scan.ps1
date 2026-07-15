<#
.SYNOPSIS
    BestPartner Harness 漂移掃描（Harness 原則 6：Entropy Management）。

.DESCRIPTION
    可重複執行的程序，逐項對照 .claude/rules/ 的黃金規範，掃描跨模組慣例漂移。
    輸出「現況 vs 規範 vs 建議」表，並區分「歷史基線」與「本次新漂移」。

    - 結構性不變量（分層依賴、命名、@Entity 位置）由 ArchUnit 在 build 階段強制，
      本掃描補足 ArchUnit 不易檢查的慣例（i18n 寫死字串、版本硬編碼、套件拼字、CDI 標註），
      並涵蓋前端跨界契約（NodeType 前後端一致）與前端測試命名（.test.ts）。
    - 歷史基線（既存且刻意容忍的漂移）預先登錄於下方 $Baseline，不視為失敗。
    - 偵測到「新漂移」時以 exit code 1 結束，可作為 CI 把關。

.EXAMPLE
    pwsh ./harness-drift-scan.ps1
#>

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$srcMain = Join-Path $root 'bestpartner-service/src/main/kotlin/tw/zipe/bastpartner'
$buildGradle = Join-Path $root 'bestpartner-service/build.gradle.kts'
$uiSrc = Join-Path $root 'bestpartner-ui/src'
$nodeTypeKt = Join-Path $srcMain 'enumerate/NodeType.kt'
$nodeTypeTs = Join-Path $uiSrc 'types/workflow.ts'

# ---- 歷史基線（既存且刻意容忍的漂移；新增程式碼不得再擴大）----
$Baseline = @{
    # @Entity 但無 Entity 後綴（嵌入式 *Id 類為 @Embeddable，另行排除）
    EntityNoSuffix      = @('LLMMcpUserSetting')
    # 仍以字面字串拋例外、尚未改用 AppMessage 的檔案
    I18nHardcodeFiles   = @('LLMStore.kt', 'LLMResource.kt')
    # build.gradle.kts 仍硬編碼版本的依賴 artifact
    VersionHardcodeArts = @('jsqlparser')
    # 前端既存以 .spec.ts 命名的測試檔（新測試一律 .test.ts，見 frontend-conventions.md）
    SpecTsBaseline      = @('ExecutionResultDrawer.spec.ts', 'OutputForm.spec.ts', 'execution.spec.ts')
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

# 規則 6：SQL 測試資料不得含真實 API 金鑰（必須遮罩，見 documentation-update-policy.md）
# 無基線——真實金鑰任何時候都應為 0，於本機 commit 前即攔下，補 GitHub push protection 的事後把關。
$sqlDir = Join-Path $root 'docs/sql'
$secretPattern = 'sk-or-v1-[A-Za-z0-9]{20,}|sk-proj-[A-Za-z0-9_-]{20,}|sk-[A-Za-z0-9]{40,}'
$secretHit = $false
foreach ($f in (Get-ChildItem $sqlDir -Filter *.sql -ErrorAction SilentlyContinue)) {
    $hits = Select-String -Path $f.FullName -Pattern $secretPattern -AllMatches
    if ($hits) {
        $secretHit = $true
        Add-Finding '金鑰外洩' 'NEW' "$($f.Name)（$(@($hits).Count) 行）" '改用佔位符 sk-or-v1-xxx／sk-proj-xxx，勿提交真實金鑰'
    }
}
if (-not $secretHit) { Add-Finding '金鑰外洩' 'OK' 'SQL 無真實金鑰' '—' }

# 規則 7：AGENTS.md 與 .claude/CLAUDE.md 內容必須一致（兩份供不同 AI CLI 讀取）
# 兩檔位於不同目錄深度，連結前綴必然不同（根目錄用 .claude/rules/、.claude/ 內用 rules/）；
# 正規化：從兩份內容剝除字面 '.claude/' 後逐位元組比對。無基線——不一致任何時候都應攔下。
$agentsMd  = Join-Path $root 'AGENTS.md'
$claudeMd  = Join-Path $root '.claude/CLAUDE.md'
if ((Test-Path $agentsMd) -and (Test-Path $claudeMd)) {
    $normalize = {
        param($path)
        (Get-Content -Path $path -Raw -Encoding utf8).Replace("`r`n", "`n").Replace('.claude/', '')
    }
    $aNorm = & $normalize $agentsMd
    $cNorm = & $normalize $claudeMd
    if ($aNorm -eq $cNorm) {
        Add-Finding '導覽一致性' 'OK' 'AGENTS.md ≡ .claude/CLAUDE.md（正規化後）' '—'
    } else {
        Add-Finding '導覽一致性' 'NEW' 'AGENTS.md 與 .claude/CLAUDE.md 內容分歧' '兩份導覽須逐字一致（僅連結前綴例外），請同步後再推送'
    }
} else {
    Add-Finding '導覽一致性' 'NEW' '缺少 AGENTS.md 或 .claude/CLAUDE.md' '兩份導覽文件皆須存在'
}

# 規則 8：Workflow NodeType 跨界一致（後端 NodeType.kt enum ≡ 前端 types/workflow.ts union）
# 無基線——節點型別是前後端硬契約，任一邊新增/移除卻未同步，畫布與引擎即失聯。
if ((Test-Path $nodeTypeKt) -and (Test-Path $nodeTypeTs)) {
    $ktRaw = Get-Content -Path $nodeTypeKt -Raw
    # 取 enum body（大括號內），再抓大寫識別字（排除 enum class NodeType 標頭本身）
    $ktBody = if ($ktRaw -match '(?s)enum class NodeType\s*\{(.+?)\}') { $Matches[1] } else { '' }
    $ktMembers = [regex]::Matches($ktBody, '\b[A-Z][A-Z0-9_]+\b') | ForEach-Object { $_.Value } | Sort-Object -Unique
    # 前端 union：export type NodeType = 之後的 'XXX' 字面，止於下一個空白行
    $tsRaw = Get-Content -Path $nodeTypeTs -Raw
    $tsBody = if ($tsRaw -match "(?s)export type NodeType\s*=(.+?)\r?\n\r?\n") { $Matches[1] } else { '' }
    $tsMembers = [regex]::Matches($tsBody, "'([A-Z0-9_]+)'") | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique

    $onlyKt = @($ktMembers | Where-Object { $_ -notin $tsMembers })
    $onlyTs = @($tsMembers | Where-Object { $_ -notin $ktMembers })
    if ($ktMembers.Count -eq 0 -or $tsMembers.Count -eq 0) {
        Add-Finding 'NodeType 契約' 'NEW' '無法解析 NodeType 定義' '檢查 NodeType.kt 與 types/workflow.ts 格式'
    } elseif ($onlyKt.Count -eq 0 -and $onlyTs.Count -eq 0) {
        Add-Finding 'NodeType 契約' 'OK' "前後端一致（$($ktMembers.Count) 種）" '—'
    } else {
        $detail = @()
        if ($onlyKt) { $detail += "僅後端有：$($onlyKt -join ',')" }
        if ($onlyTs) { $detail += "僅前端有：$($onlyTs -join ',')" }
        Add-Finding 'NodeType 契約' 'NEW' ($detail -join '；') '同步後端 NodeType.kt 與前端 types/workflow.ts，兩邊成員須一致'
    }
} else {
    Add-Finding 'NodeType 契約' 'NEW' '缺少 NodeType.kt 或 types/workflow.ts' '兩份節點型別定義皆須存在'
}

# 規則 9：前端新測試檔一律 .test.ts（既存 .spec.ts 為基線，見 frontend-conventions.md）
if (Test-Path $uiSrc) {
    $specFiles = Get-ChildItem $uiSrc -Recurse -Filter *.spec.ts -ErrorAction SilentlyContinue
    $newSpec = @($specFiles | Where-Object { $Baseline.SpecTsBaseline -notcontains $_.Name })
    foreach ($s in $newSpec) {
        Add-Finding '測試命名' 'NEW' $s.Name '新測試檔請以 .test.ts 命名，勿新增 .spec.ts'
    }
    $baseSpec = @($specFiles | Where-Object { $Baseline.SpecTsBaseline -contains $_.Name })
    if ($baseSpec.Count -gt 0 -and $newSpec.Count -eq 0) {
        Add-Finding '測試命名' 'BASELINE' "$($baseSpec.Count) 個既存 .spec.ts" '新測試改用 .test.ts（不追溯改名既有檔）'
    } elseif ($newSpec.Count -eq 0) {
        Add-Finding '測試命名' 'OK' '無新增 .spec.ts' '—'
    }
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
