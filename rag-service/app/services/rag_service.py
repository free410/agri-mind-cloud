import uuid
from datetime import datetime

from app.core.config import settings
from app.schemas.qdrant import AgricultureMetadataFilter, QdrantSearchRequest
from app.schemas.rag import (
    BusinessCitation,
    BuiltBusinessContext,
    KnowledgeCitation,
    ModelStructuredOutput,
    QueryPlan,
    RagAnswerRequest,
    RagAnswerResponse,
    RagBriefingRequest,
    RagBriefingResponse,
    RagReferenceItem,
    RagSearchRequest,
    RagSearchResponse,
    RetrievedKnowledgeChunk,
    today_str,
)
from app.services.business_context_provider import business_context_provider
from app.services.context_builder import business_context_builder
from app.services.embedding.factory import get_embedding_provider
from app.services.model_service import model_service
from app.services.prompt_builder import prompt_builder
from app.services.qdrant_service import qdrant_service
from app.services.query_planner import query_planner


class RagService:
    def __init__(self):
        self.embedding_provider = get_embedding_provider()

    def search(self, request: RagSearchRequest) -> RagSearchResponse:
        # search 保持轻量：优先走 Qdrant 检索 knowledge，business 仍用 mock 兜底
        knowledge_items: list[RagReferenceItem] = []
        business_items: list[RagReferenceItem] = []

        if "knowledge" in request.channels:
            knowledge_items = self._search_knowledge(query=request.query, top_k=request.top_k, topic_hint=None)
        if "business" in request.channels:
            business_items = self._mock_business_hits(request.query)

        all_items = (knowledge_items + business_items)[: request.top_k]
        return RagSearchResponse(query=request.query, total=len(all_items), items=all_items)

    def answer(self, request: RagAnswerRequest) -> RagAnswerResponse:
        session_id = request.session_id or f"rag-{uuid.uuid4().hex[:12]}"
        user_query = request.query or ""

        plan = query_planner.plan(request)

        business_context = self._resolve_business_context(request, plan)
        knowledge_refs = self._resolve_knowledge_refs(request, plan, user_query)
        prompt = prompt_builder.build(
            user_query=user_query,
            plan=plan,
            business_context=business_context,
            knowledge_refs=knowledge_refs,
            knowledge_graph_context=request.knowledge_graph_context,
        )
        structured = model_service.generate_structured_answer(
            prompt=prompt,
            plan=plan,
            business_context=business_context,
            knowledge_refs=knowledge_refs,
        )

        cited_business_context = self._build_business_citation(request, business_context)
        cited_knowledge_sources = self._build_knowledge_citations(knowledge_refs)

        return RagAnswerResponse(
            session_id=session_id,
            mode=request.mode,
            question_type=plan.question_type,
            answer_summary=structured.answer_summary,
            conclusion=structured.conclusion,
            possible_causes=structured.possible_causes,
            recommendations=structured.recommendations,
            priority=structured.priority,
            should_manual_review=structured.should_manual_review,
            cited_business_context=cited_business_context,
            cited_knowledge_sources=cited_knowledge_sources,
            debug_prompt=prompt if settings.app_debug else None,
            references=knowledge_refs,
            business_context=business_context,
        )

    def briefing(self, request: RagBriefingRequest) -> RagBriefingResponse:
        query = request.focus or "今日异常与处置进展"
        refs = self.search(
            RagSearchRequest(query=query, top_k=request.top_k, channels=["business", "knowledge"])
        )
        return RagBriefingResponse(
            title=self._briefing_title(request.briefing_type),
            date=request.date or today_str(),
            summary="今日异常事件已汇总，重点风险已定位并给出处置建议，建议继续提升处置闭环效率。",
            key_points=[
                "高风险预警需优先处理",
                "设备离线与土壤湿度偏低是当前重点",
                "建议按 SOP 执行并完整记录处理日志",
            ],
            actions=[
                "联动设备巡检并确认恢复状态",
                "复核关键农田环境指标",
                "输出闭环报告用于答辩展示",
            ],
            references=refs.items,
        )

    def _resolve_business_context(self, request: RagAnswerRequest, plan: QueryPlan) -> BuiltBusinessContext | None:
        if not plan.use_business_context:
            return business_context_builder.build(plan.question_type, None)

        payload = request.business_context
        if payload is None and request.pull_business_context:
            hint = dict(request.business_context_hint)
            if request.related_warning_id is not None:
                hint["related_warning_id"] = request.related_warning_id
            if request.related_device_id is not None:
                hint["related_device_id"] = request.related_device_id
            if request.related_farmland_id is not None:
                hint["related_farmland_id"] = request.related_farmland_id
            payload = business_context_provider.fetch(user_query=request.query or "", hint=hint)

        return business_context_builder.build(plan.question_type, payload)

    def _resolve_knowledge_refs(
        self, request: RagAnswerRequest, plan: QueryPlan, user_query: str
    ) -> list[RagReferenceItem]:
        if request.retrieved_knowledge_chunks:
            return [
                self._map_chunk_to_reference(chunk) for chunk in request.retrieved_knowledge_chunks[: plan.knowledge_top_k]
            ]

        if not plan.use_knowledge_retrieval:
            return []

        return self._search_knowledge(query=user_query, top_k=plan.knowledge_top_k, topic_hint=plan.topic_hint)

    def _search_knowledge(self, query: str, top_k: int, topic_hint: str | None) -> list[RagReferenceItem]:
        try:
            query_vector = self.embedding_provider.embed_query(query)
            metadata_filter = AgricultureMetadataFilter(topic=topic_hint) if topic_hint else None
            search_result = qdrant_service.similarity_search(
                QdrantSearchRequest(
                    query_embedding=query_vector,
                    top_k=top_k,
                    filter=metadata_filter,
                )
            )
            return [
                RagReferenceItem(
                    id=item.id,
                    channel="knowledge",
                    source=str(item.metadata.get("source_name") or "knowledge://qdrant"),
                    title=str(item.metadata.get("title") or "知识片段"),
                    content=item.text,
                    score=item.score,
                    metadata=item.metadata,
                )
                for item in search_result.items
            ]
        except Exception:
            if settings.knowledge_retrieval_fallback_to_mock:
                return self._mock_knowledge_hits(query)[:top_k]
            raise

    @staticmethod
    def _map_chunk_to_reference(chunk: RetrievedKnowledgeChunk) -> RagReferenceItem:
        return RagReferenceItem(
            id=chunk.id,
            channel="knowledge",
            source=chunk.source or "knowledge://external",
            title=chunk.title,
            content=chunk.content,
            score=chunk.score,
            metadata=chunk.metadata,
        )

    @staticmethod
    def _build_business_citation(
        request: RagAnswerRequest, business_context: BuiltBusinessContext | None
    ) -> BusinessCitation | None:
        if business_context is None or not business_context.has_business_data:
            return None
        return BusinessCitation(
            summary_blocks=business_context.summary_blocks,
            risk_signals=business_context.risk_signals,
            related_ids={
                "related_warning_id": request.related_warning_id,
                "related_device_id": request.related_device_id,
                "related_farmland_id": request.related_farmland_id,
            },
        )

    @staticmethod
    def _build_knowledge_citations(references: list[RagReferenceItem]) -> list[KnowledgeCitation]:
        return [
            KnowledgeCitation(
                id=item.id,
                title=item.title,
                source=item.source,
                score=item.score,
                topic=item.metadata.get("topic"),
            )
            for item in references
        ]

    @staticmethod
    def _briefing_title(briefing_type: str) -> str:
        mapping = {
            "daily_exception": "今日异常简报",
            "warning_disposal": "预警处置简报",
            "closure_summary": "闭环总结简报",
        }
        return mapping.get(briefing_type, "RAG 简报")

    @staticmethod
    def _mock_knowledge_hits(query: str) -> list[RagReferenceItem]:
        return [
            RagReferenceItem(
                id="k-001",
                channel="knowledge",
                source="knowledge://crop/sop/tomato-leaf-yellow",
                title="番茄叶片发黄处置 SOP",
                content=f"针对问题“{query}”，建议先排查水肥平衡、根系健康和病虫害。",
                score=0.91,
                metadata={"topic": "crop"},
            ),
            RagReferenceItem(
                id="k-002",
                channel="knowledge",
                source="knowledge://device/manual/greenhouse-fan",
                title="温室风机设备巡检说明",
                content="风机异常时建议检查供电、通讯、传感器联动与控制策略。",
                score=0.88,
                metadata={"topic": "device"},
            ),
        ]

    @staticmethod
    def _mock_business_hits(query: str) -> list[RagReferenceItem]:
        now_text = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        return [
            RagReferenceItem(
                id="b-001",
                channel="business",
                source="business://warning/latest",
                title="最新高风险预警",
                content=f"{now_text} 检测到土壤湿度偏低预警，建议优先处理。",
                score=0.94,
                metadata={"table": "warning", "level": "高"},
            ),
            RagReferenceItem(
                id="b-002",
                channel="business",
                source="business://inspection/recent",
                title="最近巡检记录",
                content=f"围绕问题“{query}”已发现设备离线与异常点，建议联动巡检。",
                score=0.86,
                metadata={"table": "inspection_task_log", "status": "SUCCESS"},
            ),
        ]


rag_service = RagService()
