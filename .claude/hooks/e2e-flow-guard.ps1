<#
.SYNOPSIS
    BestPartner E2E（UI 測試）流程守門 hook。

.DESCRIPTION
    以單一腳本承接多個 Claude Code hook 事件，把 e2e-test-confirmation skill 的
    流程關卡機械化：

      UserPromptSubmit → 偵測「要跑 UI 測試」的語句 → 建立狀態檔並注入強制流程
                          （另負責捕捉使用者對修正方案的核准／否決，與 [e2e-off] 逃生門）
      PreToolUse       → 未確實停過服務就要開瀏覽器 / 未經核准就要改產品程式碼 → exit 2 擋下
      PostToolUse      → 觀察「實際發生的事」推進 phase（port 真的空了、有在問使用者）
      Stop             → 報告未完成 / 服務未關閉就要收工 → block 要求補完（有次數上限）
      SessionEnd       → 清除狀態檔並提醒殘留服務
      Mark             → 供 skill 回寫狀態（phase / journeys / reportPath / runTag / round）
      Status           → 除錯用，印出目前狀態

    ⚠️ 本腳本是 Claude Code 專屬設定的一部分，不適用「scripts/ 下 .sh + .ps1 雙實作」
    規範（見 .claude/hooks/README.md）。

.NOTES
    hook 絕不可讓使用者的一般工作中斷：除 PreToolUse 的刻意阻擋外，
    任何非預期例外一律吞掉並 exit 0（fail-open）。
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('UserPromptSubmit', 'PreToolUse', 'PostToolUse', 'Stop', 'SessionEnd', 'Mark', 'Status')]
    [string]$HookEvent,

    [string]$Field,
    [string]$Value,
    [string]$SessionId
)

try { [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false) } catch { }

# --- 常數 -------------------------------------------------------------------

# 停服務範圍：後端 80、前端 vite dev 5173 / preview 4173（DB 容器不在此列）
$script:Ports = @(80, 5173, 4173)
$script:StateDir = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Temp\claude\bestpartner-e2e'
# 流程進行中的 phase（closed 以外皆視為進行中）
$script:ActivePhases = @('triggered', 'services-stopped', 'running', 'awaiting-fix', 'fixing')
$script:StopBlockLimit = 2

# 主題詞：句子在談 UI / E2E 測試
$script:TriggerPatterns = @(
    'ui\s*test', 'ui\s*測試', 'e2e', '端到端', '端對端', '前端測試', '畫面測試', '介面測試', 'playwright\s*測試'
)
# 執行動詞：必須與主題詞「同時」命中才算「要實跑」。
# 少了這道，光是提到檔名（如 tmp-ui-e2e.mjs）或談論流程就會誤觸發。
$script:RunVerbPatterns = @(
    '跑', '執行', '重測', '再測', '測一下', '開始測', '進行.{0,6}測試', '驗一下', '實測',
    '\brun\b', '\bre-?run\b', '\bexecute\b', '\bretest\b', '\bstart\b'
)
# 同時命中則不觸發（只讀 / 只寫文件 / 檔案操作 / 非 UI 測試）
$script:ExcludePatterns = @(
    '測試計畫', '測試計劃', 'test\s*plan', 'e2e-test-plan', 'e2e-test-checklist',
    '看報告', '讀報告', '檢視報告', '測試報告內容', '單元測試', 'vitest', 'api\s*測試',
    '不要跑', '別跑', '不用跑', '先不跑', '寫測試', '測試文件', 'hook',
    'commit', '刪掉', '刪除', '\.mjs', '\.spec\.', '\.test\.',
    '解釋', '說明一下', '怎麼(跑|測|用)', '如何(跑|測|使用)', '是什麼', '\bexplain\b'
)
# 逃生門：使用者明確要求解除守門
$script:EscapeHatchPattern = '\[e2e-off\]|--no-e2e-guard'
# 使用者對「修正方案」的回覆
$script:ApprovePattern = '同意|核准|確認修正|可以修|去修|修吧|開始修|approve|go ahead'
$script:RejectPattern = '不要修|先不修|暫不修|不修了|保留原樣|列為已知問題|skip\s*fix'

# PreToolUse：命中即視為「要開瀏覽器跑測試」
$script:BrowserPatterns = @('webwright', 'playwright', 'chromium', 'puppeteer')
# PreToolUse：E2E 期間一律禁止的動作
$script:ForbiddenPatterns = @(
    @{ Pattern = 'npx\s+playwright\s+test'; Reason = '執行入口是 webwright skill，不是 npx playwright test（bestpartner-ui/e2e/ 僅為選擇器與旅程參考）。' },
    @{ Pattern = 'docker\s+(compose\s+)?(down|stop|rm|kill)'; Reason = 'E2E 期間不得停掉 DB / Milvus 等容器；停服務範圍只有 port 80/5173/4173 與殘留的 Playwright 行程。' }
)
# 產品程式碼（未經使用者核准不得修改）
$script:ProductCodePattern = '(bestpartner-service[\\/]src|bestpartner-ui[\\/](src|e2e))'

$script:StopCommandPatterns = @('stop-process', 'taskkill')
$script:BrowserSkillPattern = '^(webwright)$'

# --- 共用函式 ---------------------------------------------------------------

function Get-RepoRoot {
    # .claude/hooks/x.ps1 → 上兩層即 repo 根，不依賴 $CLAUDE_PROJECT_DIR 是否被 shell 展開
    Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
}

function Initialize-StateDir {
    if (-not (Test-Path -LiteralPath $script:StateDir)) {
        New-Item -ItemType Directory -Path $script:StateDir -Force | Out-Null
    }
    $script:StateDir
}

function Get-StatePath([string]$sid) {
    $safe = if ([string]::IsNullOrWhiteSpace($sid)) { 'unknown' } else { $sid -replace '[^A-Za-z0-9_.-]', '_' }
    Join-Path (Initialize-StateDir) ("{0}.json" -f $safe)
}

function Read-State([string]$sid) {
    $p = Get-StatePath $sid
    if (Test-Path -LiteralPath $p) {
        try { return (Get-Content -LiteralPath $p -Raw -Encoding utf8 | ConvertFrom-Json) } catch { return $null }
    }
    $null
}

function Save-State($state) {
    $p = Get-StatePath $state.sessionId
    ($state | ConvertTo-Json -Depth 6) | Set-Content -LiteralPath $p -Encoding utf8
}

function Remove-State([string]$sid) {
    $p = Get-StatePath $sid
    if (Test-Path -LiteralPath $p) { Remove-Item -LiteralPath $p -Force -ErrorAction SilentlyContinue }
}

function Get-LatestState {
    # Mark / Status 由 skill 手動呼叫，拿不到 session_id：取最近修改且 24h 內的狀態檔。
    $dir = Initialize-StateDir
    Get-ChildItem -LiteralPath $dir -Filter '*.json' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -gt (Get-Date).AddHours(-24) } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Clear-StaleState {
    $dir = Initialize-StateDir
    Get-ChildItem -LiteralPath $dir -Filter '*.json' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddHours(-24) } |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue }
}

function New-State([string]$sid) {
    [pscustomobject]@{
        sessionId      = $sid
        phase          = 'triggered'
        triggeredAt    = (Get-Date).ToString('s')
        stoppedAt      = ''
        journeys       = @()
        reportPath     = ''
        runTag         = ''
        round          = 1
        awaitingUser   = $false
        stopBlockCount = 0
    }
}

function Get-StateValue($state, [string]$name, $default) {
    $prop = $state.PSObject.Properties[$name]
    if ($prop -and $null -ne $prop.Value) { return $prop.Value }
    $default
}

function Set-StateValue($state, [string]$name, $value) {
    if ($state.PSObject.Properties[$name]) { $state.$name = $value }
    else { $state | Add-Member -NotePropertyName $name -NotePropertyValue $value }
}

function Get-BusyPorts {
    $busy = @()
    foreach ($port in $script:Ports) {
        $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($conn) { $busy += $port }
    }
    , $busy
}

function Read-HookInput {
    try {
        $reader = New-Object System.IO.StreamReader([Console]::OpenStandardInput(), [Text.UTF8Encoding]::new($false))
        $raw = $reader.ReadToEnd()
        if ([string]::IsNullOrWhiteSpace($raw)) { return $null }
        return ($raw | ConvertFrom-Json)
    } catch { return $null }
}

function Test-Pattern([string]$text, [string[]]$patterns) {
    if ([string]::IsNullOrWhiteSpace($text)) { return $false }
    foreach ($p in $patterns) { if ($text -imatch $p) { return $true } }
    $false
}

function Write-JsonOutput($obj) {
    Write-Output ($obj | ConvertTo-Json -Depth 8 -Compress)
}

function Write-Context([string]$eventName, [string]$context) {
    Write-JsonOutput ([pscustomobject]@{
            hookSpecificOutput = [pscustomobject]@{
                hookEventName     = $eventName
                additionalContext = $context
            }
        })
}

function Get-ToolCommandText($toolInput) {
    if ($null -eq $toolInput) { return '' }
    $parts = @()
    foreach ($name in 'command', 'skill', 'args', 'description') {
        $prop = $toolInput.PSObject.Properties[$name]
        if ($prop -and $prop.Value) { $parts += [string]$prop.Value }
    }
    ($parts -join ' ')
}

function Get-ToolFilePath($toolInput) {
    if ($null -eq $toolInput) { return '' }
    $prop = $toolInput.PSObject.Properties['file_path']
    if ($prop -and $prop.Value) { return [string]$prop.Value }
    ''
}

function Resolve-ReportPath([string]$path) {
    if ([string]::IsNullOrWhiteSpace($path)) { return '' }
    if ([System.IO.Path]::IsPathRooted($path)) { return $path }
    Join-Path (Get-RepoRoot) $path
}

# --- 注入文字 ---------------------------------------------------------------

function Get-FlowInstruction {
    @'
[E2E 流程守門 hook] 本次請求被判定為「要實跑 UI / E2E 測試」，必須依下列流程執行，不得便宜行事：

1. 呼叫 `e2e-test-confirmation` skill，全程依其 Step 執行（webwright 為唯一執行入口，禁止 npx playwright test）。
2. 開測前先停掉所有相關服務（後端 80、前端 5173/4173、殘留 Playwright/chromium；**不得動 DB 容器**），
   並執行 `pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field phase -Value services-stopped`
   ——該指令會實測 port 是否真的空了；未通過就呼叫 webwright 會被 PreToolUse 擋下（exit 2）。
3. 用 AskUserQuestion 複選本次要測的旅程（J1–J10），未選者於確認表標 ⏭️「本次範圍外」。
4. 每個操作步驟都要截圖，並以 Markdown 圖片語法嵌入確認表證據欄（先證據、後狀態）。
5. 出現 ❌ 時立即做「前端 / 後端 / 測試腳本 / 環境」分流並查根因，寫入問題追蹤區，
   然後 `Mark -Field phase -Value awaiting-fix` 並**停下來把判定與修正方案回報使用者**。
   ⚠️ 在使用者核准前修改 bestpartner-service / bestpartner-ui 的程式碼會被 hook 擋下（exit 2）。
6. 使用者回覆同意後，hook 會把 phase 轉為 fixing，此時才可改碼；改完**重跑選定範圍的完整流程**
   （不是只重測失敗案例），`Mark -Field round -Value 2`，結果寫入「第二輪重測結果」章節。
7. 收尾：清理本輪測試資料（只刪本輪記錄的 id）→ 補完確認表（含測試結束時間）。
8. 最後關閉所有服務，確認 port 80/5173/4173 皆無 listener。報告一律寫回同一份確認表，不另產報告檔。

若本次其實不是要實跑 UI 測試（例如只是要看報告、改測試文件或討論 hook 本身），請忽略本段並照原本的請求處理；
使用者若輸入 [e2e-off] 則整個守門流程解除。
'@
}

# --- 事件處理 ---------------------------------------------------------------

function Invoke-UserPromptSubmit {
    $inp = Read-HookInput
    if ($null -eq $inp) { return }
    $prompt = [string]$inp.prompt
    $sid = [string]$inp.session_id
    $state = Read-State $sid

    # 逃生門優先於一切
    if ($prompt -imatch $script:EscapeHatchPattern) {
        if ($null -ne $state) { $state.phase = 'closed'; Save-State $state }
        Write-Context 'UserPromptSubmit' '[E2E 流程守門] 本 session 的 E2E 強制流程已解除，後續不再擋下任何動作。'
        return
    }

    # 流程進行中：清掉「等使用者」旗標，並處理修正方案的核准／否決
    if ($null -ne $state -and $state.phase -in $script:ActivePhases) {
        Set-StateValue $state 'awaitingUser' $false

        if ($state.phase -eq 'awaiting-fix') {
            if ($prompt -imatch $script:ApprovePattern) {
                $state.phase = 'fixing'
                Save-State $state
                Write-Context 'UserPromptSubmit' @'
[E2E 流程守門] 使用者已核准修正方案，現在允許修改 bestpartner-service / bestpartner-ui 的程式碼。
修完必須：後端改動重編 uber-jar、前端改動重跑 build/dev server → 停服務並重新標記 services-stopped →
Mark -Field round -Value 2 → **重跑本次選定範圍的完整流程**（不是只重測失敗案例）→
結果寫入確認表的「第二輪重測結果」與「問題分流與修正紀錄」章節。
'@
                return
            }
            if ($prompt -imatch $script:RejectPattern) {
                $state.phase = 'running'
                Save-State $state
                Write-Context 'UserPromptSubmit' @'
[E2E 流程守門] 使用者決定不修正。把該失敗登記為已知問題（問題追蹤區 + 分流紀錄，狀態維持 ❌），
不得改碼、不得重測，直接進入收尾：清理測試資料 → 補完確認表（含測試結束時間）→ 關閉服務。
'@
                return
            }
        }

        Save-State $state
        $journeys = (Get-StateValue $state 'journeys' @()) -join ','
        Write-Context 'UserPromptSubmit' ("[E2E 流程守門] E2E 流程進行中：phase={0}、round={1}、選定範圍={2}、報告={3}。收尾前不得結束（Stop hook 會擋）。" -f `
                $state.phase, (Get-StateValue $state 'round' 1), $journeys, (Get-StateValue $state 'reportPath' '(未建立)'))
        return
    }

    # 尚未進入流程：關鍵字判定（主題詞 ＋ 執行動詞 ＋ 無排除詞，三者皆須成立）
    if (-not (Test-Pattern $prompt $script:TriggerPatterns)) { return }
    if (-not (Test-Pattern $prompt $script:RunVerbPatterns)) { return }
    if (Test-Pattern $prompt $script:ExcludePatterns) { return }

    Clear-StaleState
    $state = New-State $sid
    Save-State $state
    Write-Context 'UserPromptSubmit' (Get-FlowInstruction)
}

function Invoke-PreToolUse {
    $inp = Read-HookInput
    if ($null -eq $inp) { return }
    $state = Read-State ([string]$inp.session_id)
    if ($null -eq $state) { return }                       # 未進入 E2E 流程 → 完全不干預
    if ($state.phase -notin $script:ActivePhases) { return }

    $tool = [string]$inp.tool_name
    $text = Get-ToolCommandText $inp.tool_input
    $path = Get-ToolFilePath $inp.tool_input
    $skill = ''
    if ($inp.tool_input -and $inp.tool_input.PSObject.Properties['skill']) { $skill = [string]$inp.tool_input.skill }

    $stopCmd = 'Get-NetTCPConnection -LocalPort 80,5173,4173 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }'
    $markCmd = 'pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field phase -Value services-stopped'

    # 1. E2E 期間一律禁止的動作
    foreach ($rule in $script:ForbiddenPatterns) {
        if ($text -imatch $rule.Pattern) {
            Deny-Tool @('[E2E 流程守門] 擋下：', $rule.Reason)
        }
    }

    # 2. 未經使用者核准不得改產品程式碼（先報告、後修正）
    $isProductEdit = ($tool -in @('Write', 'Edit', 'NotebookEdit')) -and
                     ($path -imatch $script:ProductCodePattern)
    if ($isProductEdit -and $state.phase -ne 'fixing') {
        Deny-Tool @(
            '[E2E 流程守門] 擋下：E2E 進行中，未經使用者核准不得修改產品程式碼。',
            "目標檔案：$path（目前 phase=$($state.phase)）",
            '',
            '請先完成失敗分流：判定前端 / 後端 / 測試腳本 / 環境，附上判定依據（API 直呼、後端日誌、',
            '瀏覽器 console、SSE 事件序列、DB 落庫）與根因、修正方案，寫入確認表的問題追蹤區，',
            '然後執行：',
            "  pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field phase -Value awaiting-fix",
            '並把方案回報使用者等待確認。使用者回覆「同意 / 確認修正 / approve」後才會放行。'
        )
    }

    # 3. 開瀏覽器跑測試前，必須確實停過服務
    $isBrowser = ($tool -eq 'Skill' -and $skill -imatch $script:BrowserSkillPattern) -or
                 (Test-Pattern $text $script:BrowserPatterns)
    if ($isBrowser -and $state.phase -eq 'triggered') {
        $busy = Get-BusyPorts
        $busyText = if ($busy.Count -gt 0) { ($busy -join ', ') } else { '（目前無 listener，只差標記）' }
        Deny-Tool @(
            '[E2E 流程守門] 擋下：尚未確認「開測前已停掉所有相關服務」就要啟動瀏覽器測試。',
            '',
            "目前仍在監聽的 port：$busyText",
            '',
            '請先執行：',
            "  $stopCmd",
            '再執行（會實測 port 是否真的空了，通過才放行）：',
            "  $markCmd",
            '',
            '接著才依 e2e-test-confirmation skill 的 Step 4 重啟後端與前端。'
        )
    }

    if ($isBrowser -and $state.phase -eq 'services-stopped') {
        $state.phase = 'running'
        Save-State $state
    }
}

function Deny-Tool([string[]]$lines) {
    [Console]::Error.WriteLine($lines -join [Environment]::NewLine)
    exit 2
}

function Invoke-PostToolUse {
    $inp = Read-HookInput
    if ($null -eq $inp) { return }
    $state = Read-State ([string]$inp.session_id)
    if ($null -eq $state) { return }
    if ($state.phase -notin $script:ActivePhases) { return }

    $tool = [string]$inp.tool_name
    $dirty = $false

    # 正在問使用者 → Stop hook 不該把「等回覆」當成偷懶收工
    if ($tool -eq 'AskUserQuestion') {
        Set-StateValue $state 'awaitingUser' $true
        $dirty = $true
    }

    # 停服務指令跑完 → 實測 port，真的空了才推進
    if ($state.phase -eq 'triggered') {
        $text = Get-ToolCommandText $inp.tool_input
        if (Test-Pattern $text $script:StopCommandPatterns) {
            if ((Get-BusyPorts).Count -eq 0) {
                $state.phase = 'services-stopped'
                $state.stoppedAt = (Get-Date).ToString('s')
                $dirty = $true
            }
        }
    }

    if ($dirty) { Save-State $state }
}

function Invoke-Stop {
    $inp = Read-HookInput
    if ($null -eq $inp) { return }
    $state = Read-State ([string]$inp.session_id)
    if ($null -eq $state) { return }
    if ($state.phase -notin $script:ActivePhases) { return }
    if ($inp.stop_hook_active -eq $true) { return }                    # 防迴圈：已因 Stop hook 續跑就不再擋
    if ((Get-StateValue $state 'awaitingUser' $false) -eq $true) { return }  # 正在等使用者回覆，合法停點
    if ($state.phase -eq 'awaiting-fix') { return }                    # 等使用者核准修正方案，合法停點
    if ([int](Get-StateValue $state 'stopBlockCount' 0) -ge $script:StopBlockLimit) {
        [Console]::Error.WriteLine('[E2E 流程守門] 已達阻擋上限，放行；請自行確認報告與服務狀態。')
        return
    }

    $pending = @()

    $reportPath = Resolve-ReportPath ([string](Get-StateValue $state 'reportPath' ''))
    if ([string]::IsNullOrWhiteSpace($reportPath)) {
        $pending += '確認表尚未建立（或未以 Mark -Field reportPath 回寫路徑）'
    } elseif (-not (Test-Path -LiteralPath $reportPath)) {
        $pending += "確認表不存在：$reportPath"
    } else {
        $content = Get-Content -LiteralPath $reportPath -Raw -Encoding utf8
        if ($content -match '\|\s*測試結束時間\s*\|\s*\|') {
            $pending += "確認表「測試結束時間」尚未填寫：$reportPath"
        }
    }

    $busy = Get-BusyPorts
    if ($busy.Count -gt 0) { $pending += ('服務尚未關閉，port 仍在監聽：' + ($busy -join ', ')) }

    if ($pending.Count -eq 0) {
        $state.phase = 'closed'
        Save-State $state
        return
    }

    Set-StateValue $state 'stopBlockCount' ([int](Get-StateValue $state 'stopBlockCount' 0) + 1)
    Save-State $state

    $nl = [Environment]::NewLine
    $reason = '[E2E 流程守門] E2E 流程尚未收尾完成，請補完後再結束：' + $nl + '- ' + ($pending -join ($nl + '- ')) +
    $nl + $nl + '完成後請執行 Step 6.5 關閉服務（port 80/5173/4173），並確認確認表的摘要、問題追蹤區與測試結束時間都已填寫。' +
    $nl + '（若你是在等使用者回覆而必須結束回合，請先用 AskUserQuestion 提問；要中止整個流程請使用者輸入 [e2e-off]）'

    Write-JsonOutput ([pscustomobject]@{ decision = 'block'; reason = $reason })
}

function Invoke-SessionEnd {
    $inp = Read-HookInput
    if ($null -eq $inp) { return }
    $sid = [string]$inp.session_id
    $state = Read-State $sid
    if ($null -ne $state -and $state.phase -in $script:ActivePhases) {
        $busy = Get-BusyPorts
        if ($busy.Count -gt 0) {
            [Console]::Error.WriteLine('[E2E 流程守門] session 結束但仍有服務在監聽：' + ($busy -join ', '))
        }
    }
    Remove-State $sid
}

function Invoke-Mark {
    if ([string]::IsNullOrWhiteSpace($Field)) { throw '缺少 -Field' }

    if ($SessionId) {
        $state = Read-State $SessionId
        if ($null -eq $state) { $state = New-State $SessionId }
    } else {
        $latest = Get-LatestState
        if ($null -eq $latest) { throw '找不到作用中的 E2E 狀態檔（尚未觸發流程？）' }
        $state = Get-Content -LiteralPath $latest.FullName -Raw -Encoding utf8 | ConvertFrom-Json
    }

    switch ($Field) {
        'phase' {
            if ($Value -eq 'fixing') {
                throw 'phase=fixing 只能由使用者核准修正方案時自動轉入（回覆「同意 / 確認修正」），不得手動標記'
            }
            if ($Value -eq 'services-stopped') {
                $busy = Get-BusyPorts
                if ($busy.Count -gt 0) {
                    [Console]::Error.WriteLine('[E2E 流程守門] 無法標記 services-stopped：port 仍在監聽：' + ($busy -join ', '))
                    exit 1
                }
                $state.stoppedAt = (Get-Date).ToString('s')
            }
            $state.phase = $Value
        }
        'journeys' { Set-StateValue $state 'journeys' @($Value -split '[,\s]+' | Where-Object { $_ }) }
        'reportPath' { Set-StateValue $state 'reportPath' $Value }
        'runTag' { Set-StateValue $state 'runTag' $Value }
        'round' { Set-StateValue $state 'round' ([int]$Value) }
        default { throw "不支援的欄位：$Field" }
    }

    Save-State $state
    Write-Output ('[E2E 流程守門] {0} = {1}（phase={2}）' -f $Field, $Value, $state.phase)
}

function Invoke-Status {
    $latest = Get-LatestState
    if ($null -eq $latest) { Write-Output '（無作用中的 E2E 狀態檔）'; return }
    Get-Content -LiteralPath $latest.FullName -Raw -Encoding utf8
    Write-Output ('busy ports: ' + ((Get-BusyPorts) -join ', '))
}

# --- 分派 -------------------------------------------------------------------

try {
    switch ($HookEvent) {
        'UserPromptSubmit' { Invoke-UserPromptSubmit }
        'PreToolUse' { Invoke-PreToolUse }
        'PostToolUse' { Invoke-PostToolUse }
        'Stop' { Invoke-Stop }
        'SessionEnd' { Invoke-SessionEnd }
        'Mark' { Invoke-Mark }
        'Status' { Invoke-Status }
    }
} catch {
    # Mark / Status 是人為呼叫，錯誤要看得見；其餘事件一律不干擾使用者。
    if ($HookEvent -in @('Mark', 'Status')) {
        [Console]::Error.WriteLine("[E2E 流程守門] $($_.Exception.Message)")
        exit 1
    }
    exit 0
}
exit 0
