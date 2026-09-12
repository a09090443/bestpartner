# Claude Code Hooks

本目錄放 Claude Code 的 hook 腳本，由 `.claude/settings.json` 的 `hooks` 區塊掛載。

> ⚠️ **此處是「規範以外」的例外，刻意為之**：`.claude/CLAUDE.md` 規定「所有可執行腳本集中於
> repo 根目錄 `scripts/`，且 `.sh` / `.ps1` 兩份實作、CI 比對結論」。
> hook 腳本不適用該規範——它們是 Claude Code 的設定（性質同 `.claude/skills/`），
> 不是專案的建置／維運腳本，不進 CI，也只在本機 Windows 開發環境生效。
> 因此只維護單份 pwsh，不做 `.sh` 雙實作。

## 腳本一覽

| 腳本 | 掛載事件 | 用途 |
|------|---------|------|
| `e2e-flow-guard.ps1` | UserPromptSubmit / PreToolUse / PostToolUse / Stop / SessionEnd | UI（E2E）測試流程守門，見下 |

---

## `e2e-flow-guard.ps1`

把 `e2e-test-confirmation` skill 的流程關卡機械化，避免「對殘留服務開測」「測完服務沒關」
「❌ 只記錄不追根因」三種常見漏洞。

### 事件行為

| 事件 | 行為 |
|------|------|
| `UserPromptSubmit` | 三個條件同時成立才觸發：① 主題詞（`ui test`／`e2e`／`端到端`／`前端測試`…）② **執行動詞**（`跑`／`執行`／`重測`／`run`…）③ 未命中排除詞（`測試計畫`／`看報告`／`單元測試`／`api 測試`／`hook`／`commit`／`刪掉`／`.mjs`／`解釋`…）。成立時建立 session 狀態檔並注入 8 步強制流程。**只注入文字，不阻擋任何事**。 |
| `PreToolUse` | 僅在狀態檔存在時作用。要啟動瀏覽器測試（`webwright` / `playwright` / `chromium`）而 phase 仍是 `triggered`（未確認停過服務）→ **exit 2 擋下**，stderr 附上該執行的指令。 |
| `PostToolUse` | 指令含 `Stop-Process` / `taskkill` 且實測 port 80/5173/4173 皆無 listener → phase 推進為 `services-stopped`。 |
| `Stop` | 流程進行中要收工時檢查「確認表存在且『測試結束時間』已填」「確認表『測試資料處置』已填（＝已問過使用者要清理還是保留）」「port 皆無 listener」，未過則 `decision: block` 要求補完。最多擋 2 次（`stopBlockCount`），並在 `stop_hook_active` 為 true 時直接放行，避免迴圈。 |
| `SessionEnd` | 刪除狀態檔；仍有 listener 時於 stderr 提醒。 |

### 狀態檔

路徑：`%LOCALAPPDATA%\Temp\claude\bestpartner-e2e\<session_id>.json`（repo 外，不進版控）。

```jsonc
{
  "sessionId": "…",
  "phase": "triggered",   // triggered → services-stopped → running → awaiting-fix → closed
  "triggeredAt": "", "stoppedAt": "",
  "journeys": ["J1", "J5"],
  "reportPath": "docs/test-confirmations/e2e-test-confirmation-….md",
  "runTag": "", "round": 1, "stopBlockCount": 0
}
```

超過 24 小時的舊狀態檔會在下次觸發時自動清除。

### 供 skill 回寫狀態（`Mark`）

```powershell
pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field <欄位> -Value <值>
```

| 欄位 | 用途 |
|------|------|
| `phase` | `services-stopped`（**會實測 port 是否真的空了，未通過 exit 1**）／`running`／`awaiting-fix`／`closed` |
| `journeys` | 本次選定旅程，逗號分隔（`J1,J5`） |
| `reportPath` | 確認表路徑（供 Stop hook 檢查完成度） |
| `runTag` | 本輪測試資料標記 |
| `round` | 測試輪次（修正後重測為 2） |

未帶 `-SessionId` 時取「最近修改且 24 小時內」的狀態檔，因此 skill 內直接呼叫即可。

### 除錯

```powershell
# 看目前狀態與 port 佔用
pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Status

# 手動餵事件（不需啟動 Claude Code）
'{"session_id":"t1","prompt":"跑 ui test"}' | pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent UserPromptSubmit
# 反例（應無輸出）：主題詞有但無執行動詞
'{"session_id":"t2","prompt":"把 tmp-ui-e2e.mjs 刪掉"}' | pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent UserPromptSubmit
'{"session_id":"t1","tool_name":"Skill","tool_input":{"skill":"webwright"}}' | pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent PreToolUse; $LASTEXITCODE   # 應為 2
'{"session_id":"t1","stop_hook_active":false}' | pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Stop
'{"session_id":"t1"}' | pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent SessionEnd
```

hook 是否真的被 Claude Code 呼叫，用 `claude --debug` 觀察；
`$CLAUDE_PROJECT_DIR` 若未展開（路徑變成 `/.claude/hooks/…`），改用絕對路徑即可。

### 已知限制

- 只在 Windows + PowerShell 7（`pwsh`）環境生效；Linux/macOS 上 hook 會找不到 `pwsh` 而失敗
  （不影響 Claude Code 本身運作，但守門形同關閉）。
- `Stop` hook 的「報告完成度」判定是**寬鬆訊號**（確認表存在 + 「測試結束時間」與「測試資料處置」已填），
  刻意不做嚴格逐案例掃描，避免誤擋收工。真正的完整性仍由 skill 的流程與人工複核把關。
- 「測試資料處置」的檢查只看**欄位是否非空**，擋不掉「沒問使用者就自己填一個決議」——
  該關卡的實質約束在 `test-data-retention.md` 與兩個 skill 的步驟；hook 只負責讓「忘了問」不會靜默溜過。
  對應欄位名稱若在模板中改動，本 hook 的 `Invoke-Stop` 正規式須一併改。

### ⚠️ 最容易被忽略的失效模式：hook 根本沒被呼叫

狀態檔以 **session_id** 命名，而 `PreToolUse` / `Stop` 在 `Read-State <當前 session_id>` 取到 `$null` 時
會**直接 return、完全不干預**（設計上是為了「未進入 E2E 流程就別擋人」）。這兩件事合起來會產生一個
**完全靜默**的失效模式：

> 若 hook 沒被 Claude Code 呼叫（最常見原因：`.claude/settings.json` 的 hook 設定是在**本 session 開始之後**
> 才加入或修改的——依 `.claude/CLAUDE.md`，改動後需**重開 session** 才套用），
> 就不會有「本 session 的狀態檔」，於是所有 `PreToolUse` 檢查一律放行、`Stop` 也不擋。
> **整個閘門空轉，而流程中看不出任何異狀。**

E2E 週期 **202608192201** 實際踩到：全程 92 個案例跑完、改了產品程式碼，
`PreToolUse` 從未擋過任何一次；事後才發現 `%LOCALAPPDATA%\Temp\claude\bestpartner-e2e\` 下
只有一個屬於**前一個 session** 的狀態檔。而 CLI 直接執行的 `Mark` 因為會取「最近修改的狀態檔」，
仍然乖乖更新了那個舊檔，讓紀錄看起來一切正常。

**開測前先確認 hook 真的活著**（30 秒）：

```powershell
# 1. 記下目前的狀態檔清單
Get-ChildItem "$env:LOCALAPPDATA\Temp\claude\bestpartner-e2e" | Select-Object Name, LastWriteTime
```

接著在 Claude Code 中輸入一句會觸發流程的話（例如「跑 ui test」），然後再看一次：
**應該多出一個以「當前 session id」命名的新檔**（session id 可從 scratchpad 路徑或 `claude --debug` 取得）。
若沒有多出來，就是 hook 沒被呼叫——**此時守門是關的**，請重開 session 後再確認一次。

也可以直接驗擋人行為（狀態檔存在但未標記 `services-stopped` 時應回 exit 2）：

```powershell
'{"session_id":"<當前 session id>","tool_name":"Skill","tool_input":{"skill":"webwright"}}' |
  pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent PreToolUse; $LASTEXITCODE   # 應為 2
```

> 改善方向（尚未實作）：讓 `Read-State` 在「找不到當前 session 狀態檔、但同目錄存在其他狀態檔」時
> 輸出一行可見警告，把這個失效模式從靜默變成可觀測。
