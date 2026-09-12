import { Client } from 'pg'

/**
 * DB 斷言 fixture（Q2=B：E2E 直連 Postgres 查執行紀錄）。
 *
 * 查 `bestpartner.llm_workflow_execution` 與 `..._node_execution`，驗證 execute 確實落庫。
 * 連線字串預設對齊 dev（pguser/pgpass@localhost:5432/pgdb），可由 E2E_DB_URL 覆寫。
 */

const CONN =
  process.env.E2E_DB_URL || 'postgresql://pguser:pgpass@localhost:5432/pgdb'

export interface ExecutionRecord {
  execution: {
    id: string
    status: string
    trigger_type: string
    workflow_version: number
    error_node_key: string | null
  }
  nodes: Array<{ node_key: string; node_type: string; status: string }>
}

/** 取指定 workflow 名稱的「最新一次」執行紀錄與其節點紀錄；查無回 null */
export async function fetchLatestExecution(workflowName: string): Promise<ExecutionRecord | null> {
  const client = new Client({ connectionString: CONN })
  await client.connect()
  try {
    const exec = await client.query(
      `SELECT e.id, e.status, e.trigger_type, e.workflow_version, e.error_node_key
         FROM bestpartner.llm_workflow_execution e
         JOIN bestpartner.llm_workflow w ON w.id = e.workflow_id
        WHERE w.name = $1
        ORDER BY e.created_at DESC
        LIMIT 1`,
      [workflowName],
    )
    if (exec.rowCount === 0) return null
    const nodes = await client.query(
      `SELECT node_key, node_type, status
         FROM bestpartner.llm_workflow_node_execution
        WHERE execution_id = $1
        ORDER BY seq_no`,
      [exec.rows[0].id],
    )
    return { execution: exec.rows[0], nodes: nodes.rows }
  } finally {
    await client.end()
  }
}
