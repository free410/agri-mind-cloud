"""Java ObjectMapper 可能输出 camelCase 字段名，与 Pydantic snake_case 不一致会导致 422。"""

from typing import Any

# Spring RagAnswerClientRequest / 其它客户端常用别名 → RagAnswerRequest 字段名
_RAG_ANSWER_TOP_LEVEL: dict[str, str] = {
    "businessContext": "business_context",
    "relatedWarningId": "related_warning_id",
    "relatedDeviceId": "related_device_id",
    "relatedFarmlandId": "related_farmland_id",
    "topK": "top_k",
    "userQuery": "user_query",
    "sessionId": "session_id",
    "pullBusinessContext": "pull_business_context",
    "businessContextHint": "business_context_hint",
    "needStructured": "need_structured",
    "retrievedKnowledgeChunks": "retrieved_knowledge_chunks",
    # 图谱引用字段映射（漏掉导致 subgraph 数据丢失！）
    "citedKnowledgeGraph": "cited_knowledge_graph",
}


def normalize_rag_answer_body(body: Any) -> dict[str, Any]:
    if not isinstance(body, dict):
        return {}
    out = dict(body)
    for camel, snake in _RAG_ANSWER_TOP_LEVEL.items():
        if camel not in out:
            continue
        if snake not in out:
            out[snake] = out.pop(camel)
        else:
            out.pop(camel, None)
    return out
