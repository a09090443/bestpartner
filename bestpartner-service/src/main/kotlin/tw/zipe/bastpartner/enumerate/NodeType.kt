package tw.zipe.bastpartner.enumerate

/**
 * Workflow 節點類型
 */
enum class NodeType {
    TRIGGER,
    LLM_ASSISTANT,
    TOOL,
    MCP_SERVER,
    SKILL,
    KNOWLEDGE_RAG,
    CONDITION,
    LOOP,
    CODE,
    HTTP_REQUEST,
    DATA_TRANSFORM,
    OUTPUT
}
