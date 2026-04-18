# 文件更新規範

**完成任何套件異動、API 新增/修改、資料表變更後，宣告完成前必須呼叫 `documentation-sync` skill。**

該 skill 會自動讀取 git diff 判斷影響範圍，掃描 `docs-site/`、`.claude/rules/`、`README.md` 等相關文件，列出需要更新的清單並詢問是否一起更新。
