import logging
from functools import lru_cache

from app.core.config import settings
from app.services.embedding.base import EmbeddingProvider
from app.services.embedding.mock_provider import MockEmbeddingProvider
from app.services.embedding.openai_provider import OpenAIEmbeddingProvider

logger = logging.getLogger(__name__)


@lru_cache
def get_embedding_provider() -> EmbeddingProvider:
    provider = settings.embedding_provider.lower().strip()
    if provider == "openai":
        if not (settings.openai_api_key or "").strip():
            logger.warning(
                "EMBEDDING_PROVIDER=openai 但未配置 OPENAI_API_KEY，已改用本地 mock 向量（仅用于演示）。"
                "接入 Qdrant 检索请配置 Key 或显式设置 EMBEDDING_PROVIDER=mock。"
            )
            return MockEmbeddingProvider(vector_size=settings.qdrant_vector_size)
        return OpenAIEmbeddingProvider(
            model=settings.embedding_model,
            api_key=settings.openai_api_key,
            vector_size=settings.qdrant_vector_size,
            base_url=settings.openai_base_url,
            dimensions=settings.embedding_dimensions,
            timeout_seconds=settings.openai_timeout_seconds,
        )
    return MockEmbeddingProvider(vector_size=settings.qdrant_vector_size)
