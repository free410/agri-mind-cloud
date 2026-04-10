from functools import lru_cache

from qdrant_client import QdrantClient

from app.core.qdrant_config import get_qdrant_config


@lru_cache
def get_qdrant_client() -> QdrantClient:
    cfg = get_qdrant_config()
    return QdrantClient(
        url=cfg.endpoint,
        api_key=cfg.api_key or None,
        timeout=cfg.timeout_seconds,
    )
