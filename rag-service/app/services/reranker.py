"""
Reranker（重排序）服务：使用交叉编码器对初步检索结果进行二次排序，
提升Top-K结果的相关性精度。
"""

import logging
from dataclasses import dataclass
from typing import Literal

import httpx

logger = logging.getLogger(__name__)


# 配置默认值（模块加载时使用，运行时会被单例覆盖）
_DEFAULT_MIN_SCORE = 0.5
_DEFAULT_TOP_K = 3


@dataclass
class RerankResult:
    id: str
    original_index: int
    rerank_score: float
    text: str
    metadata: dict


class RerankerService:
    """
    重排序服务：使用 BGE-Reranker 对检索结果进行二次排序。

    支持的Reranker模型：
    - BAAI/bge-reranker-v2-m3（推荐，本地部署或使用API）
    - Cohere rerank（云端API）
    - Jina reranker（云端API）

    配置项：
    - RERANKER_ENABLED: 是否启用重排序
    - RERANKER_API_URL: Reranker服务地址
    - RERANKER_MODEL: 使用的模型
    - RERANKER_API_KEY: API密钥
    """

    def __init__(
        self,
        enabled: bool = True,
        api_url: str | None = None,
        model: str = "BAAI/bge-reranker-v2-m3",
        api_key: str = "",
        top_k: int = 3,
    ):
        self.enabled = enabled
        self.api_url = api_url or getattr(settings, 'reranker_api_url', 'http://localhost:8000/rerank')
        self.model = model
        self.api_key = api_key
        self.top_k = top_k

    def rerank(
        self,
        query: str,
        documents: list[dict],  # [{"id": str, "text": str, "score": float, "metadata": dict}]
        top_k: int | None = None,
    ) -> list[RerankResult]:
        """
        对文档列表进行重排序。

        Args:
            query: 用户查询
            documents: 待排序的文档列表
            top_k: 返回前k个结果，默认使用初始化时的top_k

        Returns:
            重排序后的结果列表
        """
        if not self.enabled:
            logger.debug("Reranker未启用，直接返回原始顺序")
            return [
                RerankResult(
                    id=doc.get("id", ""),
                    original_index=i,
                    rerank_score=doc.get("score", 0.0),
                    text=doc.get("text", ""),
                    metadata=doc.get("metadata", {}),
                )
                for i, doc in enumerate(documents)
            ][:top_k or self.top_k]

        if not documents:
            return []

        effective_top_k = top_k or self.top_k

        # 调用Reranker API
        try:
            reranked = self._call_reranker_api(query, documents, effective_top_k)
            return reranked
        except Exception as e:
            logger.warning(f"Reranker API调用失败: {e}，降级为原始排序")
            return [
                RerankResult(
                    id=doc.get("id", ""),
                    original_index=i,
                    rerank_score=doc.get("score", 0.0),
                    text=doc.get("text", ""),
                    metadata=doc.get("metadata", {}),
                )
                for i, doc in enumerate(documents)
            ][:effective_top_k]

    def _call_reranker_api(
        self,
        query: str,
        documents: list[dict],
        top_k: int,
    ) -> list[RerankResult]:
        """
        调用远程Reranker服务。

        支持多种API格式：
        1. BGE-Reranker API格式
        2. Cohere格式
        3. Jina格式
        """
        texts = [doc.get("text", "") for doc in documents]

        # 尝试BGE-Reranker格式
        try:
            return self._call_bge_reranker(query, texts, documents, top_k)
        except Exception as e:
            logger.debug(f"BGE-Reranker格式调用失败: {e}")

        # 尝试Cohere格式
        try:
            return self._call_cohere_reranker(query, texts, documents, top_k)
        except Exception as e:
            logger.debug(f"Cohere格式调用失败: {e}")

        # 降级：使用原始分数
        logger.warning("所有Reranker API调用失败，使用原始排序")
        raise RuntimeError("Reranker API不可用")

    def _call_bge_reranker(
        self,
        query: str,
        texts: list[str],
        original_docs: list[dict],
        top_k: int,
    ) -> list[RerankResult]:
        """调用BGE-Reranker API（本地部署）"""
        payload = {
            "query": query,
            "texts": texts,
            "top_k": top_k,
        }
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

        with httpx.Client(timeout=30.0) as client:
            resp = client.post(self.api_url, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()

        results = data.get("results", data.get("result", []))
        rerank_results = []
        for item in results:
            idx = item.get("index", 0)
            doc = original_docs[idx]
            rerank_results.append(
                RerankResult(
                    id=doc.get("id", ""),
                    original_index=idx,
                    rerank_score=float(item.get("rerank_score", item.get("score", 0.0))),
                    text=doc.get("text", ""),
                    metadata=doc.get("metadata", {}),
                )
            )
        return rerank_results

    def _call_cohere_reranker(
        self,
        query: str,
        texts: list[str],
        original_docs: list[dict],
        top_k: int,
    ) -> list[RerankResult]:
        """调用Cohere Rerank API"""
        cohere_api_key = self.api_key or getattr(settings, 'cohere_api_key', '')
        cohere_url = "https://api.cohere.ai/v1/rerank"

        payload = {
            "query": query,
            "documents": texts,
            "top_n": top_k,
            "model": "rerank-english-v2.0",
        }
        headers = {
            "Authorization": f"Bearer {cohere_api_key}",
            "Content-Type": "application/json",
        }

        with httpx.Client(timeout=30.0) as client:
            resp = client.post(cohere_url, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()

        results = data.get("results", [])
        rerank_results = []
        for item in results:
            idx = item.get("index", 0)
            doc = original_docs[idx]
            rerank_results.append(
                RerankResult(
                    id=doc.get("id", ""),
                    original_index=idx,
                    rerank_score=float(item.get("relevance_score", 0.0)),
                    text=doc.get("text", ""),
                    metadata=doc.get("metadata", {}),
                )
            )
        return rerank_results


class SimilarityFilter:
    """
    相似度阈值过滤器：过滤掉低相似度的检索结果。
    """

    def __init__(self, min_score: float = 0.5):
        """
        Args:
            min_score: 最低相似度阈值，默认0.5
        """
        self.min_score = min_score

    def filter(
        self,
        items: list[dict],  # [{"id": str, "text": str, "score": float, "metadata": dict}]
        min_score: float | None = None,
    ) -> tuple[list[dict], list[dict]]:
        """
        过滤低相似度结果。

        Args:
            items: 待过滤的检索结果
            min_score: 临时覆盖的阈值

        Returns:
            (passed, filtered): 通过过滤的结果，被过滤的结果
        """
        threshold = min_score if min_score is not None else self.min_score
        passed = []
        filtered = []

        for item in items:
            score = item.get("score", 0.0)
            if score >= threshold:
                passed.append(item)
            else:
                filtered.append(item)

        return passed, filtered

    def log_filter_stats(
        self,
        query: str,
        domain: str,
        total: int,
        passed: int | list[dict],
        filtered: int | list[dict],
    ) -> None:
        """记录过滤统计日志，便于后续优化阈值"""
        # 统一处理：如果是list则取长度
        passed_count = passed if isinstance(passed, int) else len(passed)
        filtered_count = filtered if isinstance(filtered, int) else len(filtered)
        passed_items = passed if isinstance(passed, list) else []
        filtered_items = filtered if isinstance(filtered, list) else []

        if not filtered_items:
            logger.info(
                f"[检索过滤] query={query[:50]}... domain={domain} "
                f"total={total} passed={passed_count} filtered=0"
            )
        else:
            min_passed = passed_items[-1].get("score", 0) if passed_items else float('inf')
            max_filtered = filtered_items[0].get("score", 0) if filtered_items else 0
            logger.info(
                f"[检索过滤] query={query[:50]}... domain={domain} "
                f"total={total} passed={passed_count} filtered={filtered_count} "
                f"threshold={self.min_score} "
                f"min_passed={min_passed:.4f} max_filtered={max_filtered:.4f}"
            )
            # 记录被过滤项的详情（用于分析）
            for item in filtered_items[:3]:  # 最多记录3条
                logger.debug(
                    f"[检索过滤-丢弃] id={item.get('id')} score={item.get('score', 0):.4f} "
                    f"title={item.get('metadata', {}).get('title', 'N/A')[:30]}"
                )


# 全局单例（延迟初始化避免循环导入）
_reranker_service_instance = None
_similarity_filter_instance = None


def _get_reranker_service():
    global _reranker_service_instance, _similarity_filter_instance
    if _reranker_service_instance is None:
        try:
            from app.core.config import settings
            _reranker_service_instance = RerankerService(
                enabled=getattr(settings, 'reranker_enabled', False),
                api_url=getattr(settings, 'reranker_api_url', 'http://localhost:8000/rerank'),
                model=getattr(settings, 'reranker_model', 'BAAI/bge-reranker-v2-m3'),
                api_key=getattr(settings, 'reranker_api_key', ''),
                top_k=getattr(settings, 'retrieval_rerank_top_k', _DEFAULT_TOP_K),
            )
            _similarity_filter_instance = SimilarityFilter(
                min_score=getattr(settings, 'retrieval_min_similarity_score', _DEFAULT_MIN_SCORE)
            )
        except ImportError:
            # 配置未加载时使用默认值
            _reranker_service_instance = RerankerService(enabled=False, top_k=_DEFAULT_TOP_K)
            _similarity_filter_instance = SimilarityFilter(min_score=_DEFAULT_MIN_SCORE)
    return _reranker_service_instance


def _get_similarity_filter():
    _get_reranker_service()  # 确保已初始化
    return _similarity_filter_instance


class LazyReranker:
    """延迟加载的Reranker服务代理"""
    def __getattr__(self, name):
        return getattr(_get_reranker_service(), name)


class LazyFilter:
    """延迟加载的过滤器代理"""
    def __getattr__(self, name):
        return getattr(_get_similarity_filter(), name)


# 为了向后兼容，提供直接访问
reranker_service = LazyReranker()
similarity_filter = LazyFilter()
