from app.schemas.rag import QueryPlan, RagAnswerRequest
from app.services.context_builder import business_context_builder


class RagQueryPlanner:
    """
    Query Router / Planner：
    负责将输入问题路由到 business / knowledge / hybrid。
    """

    def plan(self, request: RagAnswerRequest) -> QueryPlan:
        query = request.query or ""
        mode = request.mode

        # 纯业务问答：不拉知识库片段，避免检索到与预警/设备无关的农业资讯条目干扰阅读
        if mode == "business":
            return QueryPlan(
                question_type="warning",
                mode=mode,
                use_business_context=True,
                use_knowledge_retrieval=False,
                knowledge_top_k=request.top_k,
                topic_hint="warning",
            )

        if mode in ("knowledge", "hybrid"):
            question_type = mode
        else:
            question_type = business_context_builder.classify_question(query)

        has_business_hint = any(
            value is not None
            for value in (request.related_warning_id, request.related_device_id, request.related_farmland_id)
        )

        if question_type == "warning":
            use_business = True
            use_knowledge = True
            topic_hint = "warning"
        elif question_type == "knowledge":
            use_business = request.business_context is not None or request.pull_business_context or has_business_hint
            use_knowledge = True
            topic_hint = "crop"
        else:
            use_business = True
            use_knowledge = True
            topic_hint = "warning"

        return QueryPlan(
            question_type=question_type,  # type: ignore[arg-type]
            mode=mode,
            use_business_context=use_business,
            use_knowledge_retrieval=use_knowledge,
            knowledge_top_k=request.top_k,
            topic_hint=topic_hint,
        )


query_planner = RagQueryPlanner()
