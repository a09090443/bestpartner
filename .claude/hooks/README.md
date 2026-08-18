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
| `Stop` | 流程進行中要收工時檢查「確認表存在且『測試結束時間』已填」「port 皆無 listener」，未過則 `decision: block` 要求補完。最多擋 2 次（`stopBlockCount`），並在 `stop_hook_active` 為 true 時直接放行，避免迴圈。 |
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
- `Stop` hook 的「報告完成度」判定是**寬鬆訊號**（確認表存在 + 「測試結束時間」已填），
  刻意不做嚴格逐案例掃描，避免誤擋收工。真正的完整性仍由 skill 的流程與人工複核把關。
