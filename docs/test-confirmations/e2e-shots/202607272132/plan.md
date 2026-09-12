# Critical Points — KNOWLEDGE_RAG 作為 LLM 外掛（UI-driven E2E，報告時戳 202607272132）

任務：以真實瀏覽器操作 BestPartner workflow 編輯器，驗證「知識庫 RAG 節點可連到 LLM 助手的
工具輸入埠（`in:tool`）作為自動注入型 RAG 外掛」，並對每個步驟留下截圖證據。

- [ ] CP1: 登入頁可載入，以 admin 帳號登入成功並導向 workflow 清單
- [ ] CP2: 建立新 workflow，進入編輯器，流程名稱可填入
- [ ] CP3: 由左側 palette 拖入 5 個節點（TRIGGER / LLM_ASSISTANT / OUTPUT / 2×KNOWLEDGE_RAG）
- [ ] CP4: 連 3 條線：TRIGGER→LLM(in:main)、RAG1→LLM(**in:tool**)、RAG2→LLM(**in:tool**)，
        且 UI 不出現相容性錯誤（本次改動核心：RAG 可掛工具埠）
- [ ] CP5: 兩個 KNOWLEDGE_RAG 節點**只填知識庫（knowledgeId）**，query / embeddingModelId 留空
- [ ] CP6: LLM 節點設定 llmId + userPrompt（只問問題，不手動插值知識）；OUTPUT 設定映射
- [ ] CP7: 存檔成功（saved-badge 出現）
- [ ] CP8: 啟用成功（active-toggle 開啟不報必填錯誤）— 驗證新契約僅需 knowledgeId
- [ ] CP9: 執行成功，ExecutionResultDrawer 顯示節點狀態轉移，最終 SUCCESS（非 FAILED/HTTP 400）
- [ ] CP10: KNOWLEDGE_RAG 作能力掛載時**不落主遍歷** — drawer 與 DB node_execution 皆無 rag 節點紀錄
- [ ] CP11: DB 落庫斷言：llm_workflow_execution 有一筆 SUCCESS；llm_workflow_node_execution
        僅 trigger/llm/output 三筆
- [ ] CP12: 測試 workflow 清理完畢（e2e-* 前綴掃描歸零）

## 已知待查
- 前次（e2e-shots/202607252225/UI-09-executed.png）UI 執行為 **FAILED / HTTP 400**，需在本次
  取得後端實際錯誤訊息並歸因。
