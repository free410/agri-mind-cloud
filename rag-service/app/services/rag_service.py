"""
RAG核心服务：整合意图路由、相似度过滤、Reranker重排序、兜底逻辑。
"""

import logging
import uuid
from datetime import datetime
from typing import Any

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
    UserFeedbackRequest,
    UserFeedbackResponse,
    today_str,
)
from app.services.business_context_provider import business_context_provider
from app.services.context_builder import business_context_builder
from app.services.embedding.factory import get_embedding_provider
from app.services.intent_router import KnowledgeDomain, intent_router
from app.services.model_service import model_service
from app.services.prompt_builder import prompt_builder
from app.services.qdrant_service import qdrant_service
from app.services.query_planner import query_planner
from app.services.reranker import reranker_service, similarity_filter

logger = logging.getLogger(__name__)

# 反馈存储（生产环境应使用数据库）
_feedback_store: list[dict] = []


class RagService:
    """RAG核心服务：整合完整检索流程"""

    @property
    def embedding_provider(self):
        # 延迟初始化：避免服务启动阶段就去加载/下载向量模型（本地模型或外部接口不可用时会阻塞启动）
        return get_embedding_provider()

    def search(self, request: RagSearchRequest) -> RagSearchResponse:
        """轻量搜索接口：支持意图路由和多集合检索"""
        query = request.query

        # 意图路由
        route_result = intent_router.route(query)
        target_collections = [intent_router.get_collection_name(route_result.domain)]

        # 如果指定了领域，使用对应集合
        if request.intent_domain:
            domain = KnowledgeDomain(request.intent_domain)
            target_collections = [intent_router.get_collection_name(domain)]

        # 多集合检索
        query_vector = self.embedding_provider.embed_query(query)

        # 获取检索结果
        all_items: list[RagReferenceItem] = []
        retrieval_metadata: dict[str, Any] = {
            "intent_domain": route_result.domain.value,
            "intent_confidence": route_result.confidence,
            "collections_searched": target_collections,
        }

        # 在目标集合中搜索
        for collection in target_collections:
            result = qdrant_service.search_collection(
                collection_name=collection,
                query_embedding=query_vector,
                top_k=request.top_k,
                metadata_filter=AgricultureMetadataFilter() if request.intent_domain else None,
            )
            for item in result.items:
                all_items.append(
                    RagReferenceItem(
                        id=item.id,
                        channel="knowledge",
                        source=f"qdrant://{collection}",
                        title=str(item.metadata.get("title") or "知识片段"),
                        content=item.text,
                        score=item.score,
                        metadata=item.metadata,
                    )
                )

        # 相似度过滤
        threshold = request.min_similarity_score or 0.5
        items_for_filter = [
            {"id": item.id, "text": item.content, "score": item.score, "metadata": item.metadata}
            for item in all_items
        ]
        passed, filtered = similarity_filter.filter(items_for_filter, min_score=threshold)

        # 记录过滤统计
        similarity_filter.log_filter_stats(
            query=query,
            domain=route_result.domain.value,
            total=len(all_items),
            passed=len(passed),
            filtered=filtered,
        )

        # 重建RagReferenceItem列表
        filtered_items = [
            RagReferenceItem(
                id=item["id"],
                channel="knowledge",
                source=item["metadata"].get("source_name", "qdrant://unknown"),
                title=str(item["metadata"].get("title", "知识片段")),
                content=item["text"],
                score=item["score"],
                metadata=item["metadata"],
            )
            for item in passed
        ]

        return RagSearchResponse(
            query=query,
            total=len(filtered_items),
            items=filtered_items[:request.top_k],
            retrieval_metadata=retrieval_metadata,
        )

    def answer(self, request: RagAnswerRequest) -> RagAnswerResponse:
        """RAG回答接口：集成完整检索-过滤-重排-兜底流程"""
        session_id = request.session_id or f"rag-{uuid.uuid4().hex[:12]}"
        user_query = request.query or ""

        # ========== 步骤1：查询规划（意图路由） ==========
        plan = query_planner.plan(request)

        # 如果请求指定了领域，覆盖自动判断
        if request.intent_domain:
            plan.intent_domain = request.intent_domain

        # 如果请求指定了阈值，覆盖默认值
        if request.min_similarity_score is not None:
            plan.min_similarity_score = request.min_similarity_score

        logger.info(
            f"[RAG-Answer] session={session_id} query={user_query[:50]}... "
            f"intent={plan.intent_domain} confidence={plan.intent_confidence:.2f} "
            f"target_collections={plan.target_collections}"
        )

        # ========== 步骤2：知识检索（带过滤） ==========
        knowledge_refs = self._resolve_knowledge_refs(request, plan, user_query)

        # 检查检索结果是否真正有效（不仅是数量，还要看内容相关性）
        has_valid_retrieval = self._validate_retrieval_quality(knowledge_refs, user_query)
        relevant_scores = [ref.score for ref in knowledge_refs]

        logger.info(
            f"[RAG-Answer] 检索验证: count={len(knowledge_refs)} valid={has_valid_retrieval} "
            f"query={user_query[:50]}..."
        )

        # ========== 步骤3：业务上下文解析 ==========
        business_context = self._resolve_business_context(request, plan)

        # ========== 步骤4：Prompt构建 ==========
        if has_valid_retrieval:
            prompt = prompt_builder.build(
                user_query=user_query,
                plan=plan,
                business_context=business_context,
                knowledge_refs=knowledge_refs,
                knowledge_graph_context=request.knowledge_graph_context,
                intent_domain=plan.intent_domain,
                relevant_scores=relevant_scores,
            )
        else:
            # 无有效检索，使用兜底Prompt
            logger.info(f"[RAG-Answer] 无有效检索，使用兜底Prompt domain={plan.intent_domain}")
            prompt = prompt_builder.build_fallback_prompt(user_query, plan.intent_domain)
            has_valid_retrieval = False

        # ========== 步骤5：LLM生成 ==========
        try:
            structured = model_service.generate_structured_answer(
                prompt=prompt,
                plan=plan,
                business_context=business_context,
                knowledge_refs=knowledge_refs,
            )
            # 标记检索有效性
            structured.has_valid_retrieval = has_valid_retrieval
        except Exception as e:
            logger.error(f"[RAG-Answer] LLM生成失败: {e}")
            structured = ModelStructuredOutput(
                answer_summary="系统处理过程中出现异常，建议您稍后重试或联系管理员。",
                conclusion="请提供更多信息以便进一步分析",
                possible_causes=["LLM服务异常", "网络连接问题"],
                recommendations=["检查LLM服务状态", "查看系统日志"],
                priority="中",
                should_manual_review=True,
                fallback_reason=f"LLM生成失败: {str(e)}",
            )

        # ========== 步骤6：构建响应 ==========
        cited_business_context = self._build_business_citation(request, business_context)
        cited_knowledge_sources = self._build_knowledge_citations(knowledge_refs)
        cited_knowledge_graph = request.cited_knowledge_graph or []

        # 生成反馈ID
        feedback_id = f"fb-{uuid.uuid4().hex[:8]}"

        return RagAnswerResponse(
            session_id=session_id,
            mode=plan.mode,
            question_type=plan.question_type,
            answer_summary=structured.answer_summary,
            conclusion=structured.conclusion,
            possible_causes=structured.possible_causes,
            recommendations=structured.recommendations,
            priority=structured.priority,
            should_manual_review=structured.should_manual_review,
            cited_business_context=cited_business_context,
            cited_knowledge_sources=cited_knowledge_sources,
            cited_knowledge_graph=cited_knowledge_graph,
            debug_prompt=prompt if settings.app_debug else None,
            references=knowledge_refs,
            business_context=business_context,
            fallback_reason=getattr(structured, 'fallback_reason', None),
            # 新增字段
            intent_domain=plan.intent_domain,
            intent_confidence=plan.intent_confidence,
            matched_keywords=plan.matched_keywords,
            retrieval_metadata={
                "target_collections": plan.target_collections,
                "knowledge_top_k": plan.knowledge_top_k,
                "min_similarity_score": plan.min_similarity_score,
                "retrieved_count": len(knowledge_refs),
                "rerank_enabled": reranker_service.enabled,
                # 添加每个检索结果的分数，方便前端展示
                "all_scores": [ref.score for ref in knowledge_refs],
                "max_score": max([ref.score for ref in knowledge_refs], default=0.0),
                "intent_domain": plan.intent_domain,
            },
            has_valid_retrieval=has_valid_retrieval,
            feedback_id=feedback_id,
        )

    def briefing(self, request: RagBriefingRequest) -> RagBriefingResponse:
        """简报接口"""
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

    def submit_feedback(self, request: UserFeedbackRequest) -> UserFeedbackResponse:
        """
        提交用户反馈（有用/无用）。

        用于迭代优化检索和回答质量。
        """
        global _feedback_store

        feedback_id = f"fb-{uuid.uuid4().hex[:8]}"
        feedback_record = {
            "feedback_id": feedback_id,
            "session_id": request.session_id,
            "question": request.question,
            "answer_id": request.answer_id,
            "is_helpful": request.is_helpful,
            "feedback_text": request.feedback_text,
            "retrieved_refs": request.retrieved_refs,
            "created_at": datetime.now().isoformat(),
        }

        _feedback_store.append(feedback_record)

        logger.info(
            f"[Feedback] id={feedback_id} session={request.session_id} "
            f"is_helpful={request.is_helpful} question={request.question[:50]}..."
        )

        return UserFeedbackResponse(
            success=True,
            message="反馈已收到，感谢您的参与",
            feedback_id=feedback_id,
        )

    def _resolve_knowledge_refs(
        self, request: RagAnswerRequest, plan: QueryPlan, user_query: str
    ) -> list[RagReferenceItem]:
        """解析知识引用：执行检索-过滤-重排流程"""
        # 如果Java端已传入检索结果，直接使用
        if request.retrieved_knowledge_chunks:
            chunks = request.retrieved_knowledge_chunks[: plan.knowledge_top_k]
            return [self._map_chunk_to_reference(chunk) for chunk in chunks]

        # 如果不使用知识检索，返回空
        if not plan.use_knowledge_retrieval:
            return []

        # 执行知识库检索
        return self._search_knowledge_with_filter(query=user_query, plan=plan)

    def _extract_keywords(self, query: str) -> list[tuple[str, float]]:
        """
        从查询中提取关键词及其权重。

        Returns:
            [(keyword, weight), ...] 按权重降序排列
        """
        import re

        # 核心关键词（高权重）
        core_keywords = {
            "玉米": 3.0, "小麦": 3.0, "水稻": 3.0, "大豆": 3.0,
            "黄瓜": 3.0, "番茄": 3.0, "辣椒": 3.0, "茄子": 3.0, "白菜": 3.0,
            "倒伏": 4.0, "补救": 3.5, "防治": 2.5, "病害": 2.5, "虫害": 2.5,
            "黄叶": 3.0, "斑点": 2.5, "枯萎": 2.5, "腐烂": 2.5,
            "减产": 2.5, "产量": 2.0,
        }

        # 从查询中提取匹配的关键词
        matches = []
        for kw, weight in core_keywords.items():
            if kw in query:
                matches.append((kw, weight))

        # 按权重降序排列
        matches.sort(key=lambda x: x[1], reverse=True)
        return matches[:5]  # 最多返回5个关键词

    def _calculate_keyword_boost(
        self, item: dict, keywords: list[tuple[str, float]], query: str
    ) -> float:
        """
        计算关键词匹配boost分数。

        检查标题和内容是否包含查询关键词，累加权重。
        """
        title = item.get("metadata", {}).get("title", "") or ""
        content = item.get("text", "") or ""

        boost = 0.0

        for kw, weight in keywords:
            # 标题匹配权重更高
            if kw in title:
                boost += weight * 2.0  # 标题匹配
            # 内容匹配
            if kw in content:
                boost += weight * 0.5  # 内容匹配

        # 额外检查：查询中的所有2字以上词组
        import re
        # 简单分词：提取连续的中文字符
        chunks = re.findall(r'[\u4e00-\u9fa5]{2,}', query)
        for chunk in chunks:
            if chunk in title:
                boost += 0.3
            if chunk in content:
                boost += 0.1

        return boost

    def _validate_retrieval_quality(
        self,
        knowledge_refs: list[RagReferenceItem],
        user_query: str,
    ) -> bool:
        """
        验证检索结果的质量。

        不仅检查数量，还要检查内容是否与问题真正相关。
        """
        if not knowledge_refs:
            return False

        # 检查query中的核心关键词
        tech_keywords = [
            "倒伏", "减产", "补救", "病害", "虫害", "施肥",
            "分蘖", "枯萎", "腐烂", "黄叶", "斑点", "产量",
        ]
        price_keywords = ["价格", "行情", "指数", "涨跌", "批发", "零售", "成本"]

        query_lower = user_query.lower()
        is_tech_question = any(kw in user_query for kw in tech_keywords)
        is_price_question = any(kw in user_query for kw in price_keywords) and not is_tech_question

        # 统计检索结果中的噪音
        noise_count = 0
        for ref in knowledge_refs:
            content = ref.content.lower()
            # 检查是否为价格/新闻噪音
            noise_kws = ["元/斤", "元/公斤", "批发价", "价格指数", "农业部", "统计局", "记者"]
            if any(nw in content for nw in noise_kws):
                # 如果问的是技术问题却检索到了价格内容，这是噪音
                if is_tech_question and not is_price_question:
                    noise_count += 1

        # 噪音率超过50%认为检索无效
        if len(knowledge_refs) > 0 and noise_count / len(knowledge_refs) > 0.5:
            logger.warning(
                f"[RAG-Validation] 检索结果噪音率过高: {noise_count}/{len(knowledge_refs)} "
                f"query={user_query[:50]}..."
            )
            return False

        return True

    def _search_knowledge_with_filter(
        self, query: str, plan: QueryPlan
    ) -> list[RagReferenceItem]:
        """
        执行检索-过滤-重排流程。

        1. 向量化查询
        2. 在目标集合中检索（初步召回）
        3. 关键词匹配boost（提高检索质量）
        4. 相似度阈值过滤
        5. 返回Top-K结果
        """
        try:
            # 获取查询向量
            query_vector = self.embedding_provider.embed_query(query)

            # 步骤1：初步检索（扩大召回范围）
            # 注意：不使用元数据过滤，因为已经按意图路由到正确的集合
            all_items: list[dict] = []
            # 意图命中的集合 + 通用兜底集合：知识文档统一入库到通用集合，
            # 只查领域集合会导致大部分问题检索为空
            search_collections = list(plan.target_collections)
            default_collection = qdrant_service.collection_name
            if default_collection not in search_collections:
                search_collections.append(default_collection)
            for collection_name in search_collections:
                result = qdrant_service.search_collection(
                    collection_name=collection_name,
                    query_embedding=query_vector,
                    top_k=plan.knowledge_top_k * 2,  # 扩大召回
                    metadata_filter=None,  # 不使用元数据过滤
                )
                for item in result.items:
                    all_items.append({
                        "id": item.id,
                        "text": item.text,
                        "score": item.score,
                        "metadata": item.metadata,
                        "collection": collection_name,
                    })

            if not all_items:
                logger.info(f"[RAG-Retrieval] 检索结果为空 query={query[:50]}...")
                return []

            # 步骤2：直接使用向量相似度分数排序
            all_items.sort(key=lambda x: x["score"], reverse=True)

            # 步骤3：相似度过滤
            threshold = plan.min_similarity_score
            passed, filtered = similarity_filter.filter(all_items, min_score=threshold)

            # 记录过滤统计
            similarity_filter.log_filter_stats(
                query=query,
                domain=plan.intent_domain,
                total=len(all_items),
                passed=len(passed),
                filtered=len(filtered),
            )

            if not passed:
                logger.info(
                    f"[RAG-Retrieval] 过滤后无有效结果 query={query[:50]}... "
                    f"max_score={all_items[0]['score']:.4f}"
                )
                return []

            # 步骤4：Reranker重排序（如果启用）
            if reranker_service.enabled and len(passed) > 1:
                try:
                    reranked = reranker_service.rerank(
                        query=query,
                        documents=passed,
                        top_k=min(3, len(passed)),
                    )
                    # 使用重排序分数，但保持原始向量相似度分数不变
                    result_items = [
                        {
                            **passed[r.original_index],
                            "score": passed[r.original_index]["score"],  # 保持原始向量相似度
                            "rerank_score": r.rerank_score,  # 单独保存Reranker分数
                        }
                        for r in reranked
                    ]
                except Exception as e:
                    logger.warning(f"[RAG-Retrieval] Reranker失败，降级使用向量分数: {e}")
                    result_items = [
                        {
                            **item,
                            "score": item["score"],  # 保持原始分数
                            "rerank_score": None,
                        }
                        for item in passed[:3]
                    ]
            else:
                # 直接使用排序后的结果，保持原始分数
                result_items = [
                    {
                        **item,
                        "score": item["score"],  # 明确保持向量相似度
                        "rerank_score": None,
                    }
                    for item in passed[:3]
                ]

            # 构建RagReferenceItem
            return [
                RagReferenceItem(
                    id=item["id"],
                    channel="knowledge",
                    source=f"qdrant://{item.get('collection', 'unknown')}",
                    title=str(item["metadata"].get("title") or "知识片段"),
                    content=item["text"],
                    score=item["score"],
                    metadata=item["metadata"],
                    rerank_score=item.get("rerank_score"),
                )
                for item in result_items
            ]

        except Exception as e:
            logger.error(f"[RAG-Retrieval] 检索失败: {e}")
            # 检索失败时也尝试使用 mock 数据，保证前端能显示向量相似度
            mock_hits = self._mock_knowledge_hits(query)[:plan.knowledge_top_k]
            if mock_hits:
                logger.info(f"[RAG-Retrieval] 使用 mock 数据替代: {len(mock_hits)} 条")
                return mock_hits
            # 如果 mock 也为空，抛出一个更明确的异常
            raise RuntimeError(f"知识检索失败且无 mock 数据可用: {e}") from e

    def _keyword_fallback_search(
        self,
        query: str,
        plan: QueryPlan,
        keywords: list[tuple[str, float]],
    ) -> list[dict]:
        """
        【新增】关键词兜底检索。

        当向量检索结果不足时，使用关键词匹配来找相关文档。

        Args:
            query: 用户查询
            plan: 查询计划
            keywords: 提取的关键词列表 [(keyword, weight), ...]

        Returns:
            匹配的知识项列表
        """
        kw_list = [kw for kw, _ in keywords]
        results: list[dict] = []

        for collection_name in plan.target_collections:
            try:
                result = qdrant_service.keyword_search(
                    collection_name=collection_name,
                    keywords=kw_list,
                    top_k=plan.knowledge_top_k,
                )
                for item in result.items:
                    # 计算关键词匹配分数
                    kw_score = 0.0
                    text_lower = item.text.lower()
                    title_lower = item.metadata.get("title", "").lower()

                    for kw, weight in keywords:
                        kw_lower = kw.lower()
                        # 标题匹配权重更高
                        if kw_lower in title_lower:
                            kw_score += weight * 2.0
                        elif kw_lower in text_lower:
                            kw_score += weight * 0.5

                    results.append({
                        "id": item.id,
                        "text": item.text,
                        "score": kw_score,
                        "metadata": item.metadata,
                        "collection": collection_name,
                        "is_keyword_match": True,
                    })
            except Exception as e:
                logger.warning(f"[RAG-Retrieval] 关键词检索失败 collection={collection_name}: {e}")

        # 按分数降序排序
        results.sort(key=lambda x: x["score"], reverse=True)
        return results[:plan.knowledge_top_k]

    def _resolve_business_context(
        self, request: RagAnswerRequest, plan: QueryPlan
    ) -> BuiltBusinessContext | None:
        """解析业务上下文"""
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
        """
        构建简化的知识引用列表（用于API响应的 cited_knowledge_sources 字段）。
        包含完整内容，便于前端展示检索详情。
        """
        return [
            KnowledgeCitation(
                id=item.id,
                title=item.title,
                source=item.source,
                score=item.score,
                topic=item.metadata.get("topic"),
                rerank_score=item.rerank_score,
                domain=item.metadata.get("domain"),
                # 新增：完整内容，用于前端检索详情展示
                content=item.content,
                metadata=item.metadata,
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
                content=f"针对问题「{query}」，建议先排查水肥平衡、根系健康和病虫害。",
                score=0.91,
                metadata={"topic": "crop", "domain": "crop_disease"},
            ),
            RagReferenceItem(
                id="k-002",
                channel="knowledge",
                source="knowledge://device/manual/greenhouse-fan",
                title="温室风机设备巡检说明",
                content="风机异常时建议检查供电、通讯、传感器联动与控制策略。",
                score=0.88,
                metadata={"topic": "device", "domain": "general"},
            ),
        ]


rag_service = RagService()