from app.schemas.rag import BuiltBusinessContext, QueryPlan, RagReferenceItem


class RagPromptBuilder:
    def build(
        self,
        user_query: str,
        plan: QueryPlan,
        business_context: BuiltBusinessContext | None,
        knowledge_refs: list[RagReferenceItem],
        knowledge_graph_context: str | None = None,
    ) -> str:
        sections: list[str] = []
        sections.append(
            "你是智慧农业智能助手，请基于给定上下文输出结构化结论；回答要充实，便于一线人员直接理解与执行。"
        )
        sections.append(f"[问题类型] {plan.question_type}")
        sections.append(f"[用户问题] {user_query}")

        if business_context and business_context.has_business_data:
            sections.append("[业务上下文摘要]")
            sections.extend(f"- {item}" for item in business_context.summary_blocks[:12])
            sections.append("[业务上下文详情]")
            sections.append(business_context.context_text or "无")
            sections.append("[风险信号]")
            sections.extend(f"- {signal}" for signal in business_context.risk_signals[:12])
        else:
            sections.append("[业务上下文] 无可用实时业务数据。")

        if knowledge_refs:
            sections.append("[知识检索片段]")
            for idx, ref in enumerate(knowledge_refs[: plan.knowledge_top_k], start=1):
                sections.append(
                    f"{idx}. 标题={ref.title} 来源={ref.source} 分数={ref.score:.3f} 内容={ref.content}"
                )
        else:
            sections.append("[知识检索片段] 未命中。")

        if knowledge_graph_context and knowledge_graph_context.strip():
            sections.append("[知识图谱关联]")
            sections.append(knowledge_graph_context.strip())
        else:
            sections.append("[知识图谱关联] 无。")

        sections.append(
            "[输出体量与质量要求] "
            "answer_summary 与 conclusion 必须写得充分，避免一句话敷衍；"
            "possible_causes 至少 4 条、recommendations 至少 5 条（有依据时可更多）；"
            "知识类问题请穿插必要机理或操作要点（与检索片段一致，勿臆造数据）；"
            "业务类问题请把预警/设备/环境与农艺建议衔接起来。"
        )
        sections.append(
            "请返回字段：answer_summary、conclusion、possible_causes、recommendations、priority、should_manual_review。"
        )
        return "\n".join(sections)


prompt_builder = RagPromptBuilder()
