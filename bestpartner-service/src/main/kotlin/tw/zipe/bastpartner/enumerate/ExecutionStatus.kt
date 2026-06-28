package tw.zipe.bastpartner.enumerate

/**
 * Workflow 單次執行整體狀態
 */
enum class ExecutionStatus {
    PENDING, RUNNING, SUCCESS, FAILED, TIMEOUT, CANCELLED
}
