import logging
import os
from functools import lru_cache

from app.core.config import settings
from app.services.embedding.base import EmbeddingProvider
from app.services.embedding.mock_provider import MockEmbeddingProvider
from app.services.embedding.openai_provider import OpenAIEmbeddingProvider

logger = logging.getLogger(__name__)


@lru_cache
def get_embedding_provider() -> EmbeddingProvider:
    # 入库脚本可用 --mock-embedding，避免 .env 中 DashScope embedding 400 时阻塞建库
    mock_env = os.getenv("INDEX_FORCE_MOCK_EMBEDDING", "").strip().lower()
    if mock_env in ("1", "true", "yes"):
        logger.info("Embedding: 使用 INDEX_FORCE_MOCK_EMBEDDING=%s，强制使用 MockEmbeddingProvider", mock_env)
        return MockEmbeddingProvider(vector_size=settings.qdrant_vector_size)

    provider = settings.embedding_provider.lower().strip()

    if provider == "local":
        # 延迟导入：sentence-transformers 为可选依赖，未安装时不影响 openai/mock 模式启动
        from app.services.embedding.local_provider import LocalEmbeddingProvider

        # 本地 embedding 模型（sentence-transformers）
        model_name = os.getenv("LOCAL_EMBEDDING_MODEL", settings.local_embedding_model)
        logger.info("Embedding: 使用本地模型 (model=%s)", model_name)
        return LocalEmbeddingProvider(model_name=model_name)

    if provider == "openai":
        if not (settings.openai_api_key or "").strip():
            logger.warning(
                "EMBEDDING_PROVIDER=openai 但未配置 OPENAI_API_KEY，已改用本地 mock 向量（仅用于演示）。"
                "接入 Qdrant 检索请配置 Key 或显式设置 EMBEDDING_PROVIDER=local。"
            )
            return MockEmbeddingProvider(vector_size=settings.qdrant_vector_size)
        logger.info("Embedding: 使用 OpenAI 兼容 Provider (model=%s, api_key_prefix=%s)",
                     settings.embedding_model, (settings.openai_api_key or "")[:8])
        return OpenAIEmbeddingProvider(
            model=settings.embedding_model,
            api_key=settings.openai_api_key,
            vector_size=settings.qdrant_vector_size,
            base_url=settings.openai_base_url,
            dimensions=settings.embedding_dimensions,
            timeout_seconds=settings.openai_timeout_seconds,
        )

    logger.info("Embedding: 使用 MockEmbeddingProvider (provider=%s)", provider)
    return MockEmbeddingProvider(vector_size=settings.qdrant_vector_size)
