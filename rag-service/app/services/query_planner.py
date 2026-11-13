"""
RAG查询规划器：负责意图分类、路由选择和检索策略规划。
整合了意图路由模块，实现知识库的领域隔离。
"""

import logging
from typing import Literal

from app.schemas.rag import QueryPlan, RagAnswerRequest
from app.services.context_builder import business_context_builder
from app.services.intent_router import KnowledgeDomain, intent_router

logger = logging.getLogger(__name__)


class RagQueryPlanner:
    """
    Query Router / Planner：
    1. 意图分类：判断用户问题属于哪个农业领域（作物病害/价格行情/政策补贴/通用）
    2. 路由选择：确定是否使用业务上下文、是否拉取知识库
    3. 检索策略：设置检索参数（目标集合、top_k、过滤器等）
    """

    # 知识库检索阈值配置
    DEFAULT_MIN_SCORE = 0.3  # 降低阈值，作物病害问题需要更多召回
    DEFAULT_TOP_K = 10       # 默认检索数量
    RERANK_TOP_K = 5         # 重排序后保留数量

    # 作物病害类问题的阈值可以更低，因为农业技术知识库规模有限
    CROP_DISEASE_MIN_SCORE = 0.2  # 作物病害问题最低阈值

    def plan(self, request: RagAnswerRequest) -> QueryPlan:
        """
        生成查询计划。

        Args:
            request: RAG请求

        Returns:
            QueryPlan: 包含意图分类、路由选择、检索策略
        """
        query = request.query or ""
        mode = request.mode

        # ========== 步骤1：意图分类 ==========
        route_result = intent_router.route(query)
        intent_domain = route_result.domain.value
        intent_confidence = route_result.confidence

        logger.info(
            f"[QueryPlan] query={query[:50]}... "
            f"intent_domain={intent_domain} confidence={intent_confidence:.2f} "
            f"matched_kws={route_result.matched_keywords[:3]}"
        )

        # ========== 步骤2：路由选择 ==========
        question_type, use_business, use_knowledge = self._determine_routing(
            mode=mode,
            query=query,
            intent_domain=intent_domain,
            request=request,
        )

        # ========== 步骤3：检索策略 ==========
        topic_hint, knowledge_top_k, target_collections, min_score = self._determine_retrieval_strategy(
            intent_domain=intent_domain,
            use_knowledge=use_knowledge,
            request=request,
        )

        return QueryPlan(
            question_type=question_type,
            mode=mode,
            use_business_context=use_business,
            use_knowledge_retrieval=use_knowledge,
            knowledge_top_k=knowledge_top_k,
            topic_hint=topic_hint,
            # 新增字段
            intent_domain=intent_domain,
            intent_confidence=intent_confidence,
            matched_keywords=route_result.matched_keywords,
            target_collections=target_collections,
            min_similarity_score=min_score,
        )

    def _determine_routing(
        self,
        mode: str,
        query: str,
        intent_domain: str,
        request: RagAnswerRequest,
    ) -> tuple[Literal["warning", "knowledge", "hybrid"], bool, bool]:
        """
        确定路由选择。

        Returns:
            (question_type, use_business, use_knowledge)
        """
        # 纯业务模式：不拉知识库片段
        if mode == "business":
            return "warning", True, False

        # 知识或混合模式：需要判断是否拉业务
        has_business_hint = any(
            value is not None
            for value in (request.related_warning_id, request.related_device_id, request.related_farmland_id)
        )

        # 作物病害类问题优先拉知识，业务上下文可选
        if intent_domain == "crop_disease" and mode in ("knowledge", "hybrid", "auto"):
            question_type = "knowledge"
            use_business = request.pull_business_context or has_business_hint
            use_knowledge = True
            return question_type, use_business, use_knowledge

        # 价格行情和政策问题：主要拉知识
        if intent_domain in ("market_price", "agri_policy"):
            question_type = "knowledge"
            use_business = False  # 这类问题通常不需要业务上下文
            use_knowledge = True
            return question_type, use_business, use_knowledge

        # 通用模式：使用业务上下文分类
        if mode in ("knowledge", "hybrid", "auto"):
            question_type = business_context_builder.classify_question(query)
            if question_type == "warning":
                use_business = True
                use_knowledge = True
            elif question_type == "knowledge":
                use_business = request.pull_business_context or has_business_hint
                use_knowledge = True
            else:
                use_business = True
                use_knowledge = True
            return question_type, use_business, use_knowledge

        # 兜底
        return "hybrid", True, True

    def _determine_retrieval_strategy(
        self,
        intent_domain: str,
        use_knowledge: bool,
        request: RagAnswerRequest,
    ) -> tuple[str | None, int, list[str], float]:
        """
        确定检索策略。

        Returns:
            (topic_hint, knowledge_top_k, target_collections, min_similarity_score)
        """
        if not use_knowledge:
            return None, 0, [], self.DEFAULT_MIN_SCORE

        # 目标集合映射
        collection_map = {
            "crop_disease": "agriculture_knowledge",
            "market_price": "agriculture_market_price",
            "agri_policy": "agriculture_agri_policy",
            "general": "agriculture_knowledge",
        }

        # 根据意图确定目标集合
        target_collections = []
        if intent_domain in collection_map:
            # 精确匹配：只查对应集合
            target_collections = [collection_map[intent_domain]]
            topic_hint = intent_domain
        else:
            # 兜底：查通用库
            target_collections = ["agriculture_knowledge"]
            topic_hint = None

        # top_k设置：作物病害问题需要更多检索结果以获取足够的防治方案
        if intent_domain == "crop_disease":
            knowledge_top_k = 15  # 病害问题需要更多候选
            min_similarity_score = self.CROP_DISEASE_MIN_SCORE  # 更低的阈值
        else:
            knowledge_top_k = request.top_k or self.DEFAULT_TOP_K
            min_similarity_score = self.DEFAULT_MIN_SCORE

        return topic_hint, knowledge_top_k, target_collections, min_similarity_score

    def get_retrieval_params(self, plan: QueryPlan) -> dict:
        """
        获取检索参数字典（用于调试和日志）。
        """
        return {
            "intent_domain": plan.intent_domain,
            "intent_confidence": plan.intent_confidence,
            "matched_keywords": plan.matched_keywords,
            "target_collections": plan.target_collections,
            "knowledge_top_k": plan.knowledge_top_k,
            "min_similarity_score": plan.min_similarity_score,
            "topic_hint": plan.topic_hint,
        }


query_planner = RagQueryPlanner()