from pydantic import BaseModel

from app.core.config import settings


class QdrantConfig(BaseModel):
    endpoint: str
    api_key: str
    collection_name: str
    vector_size: int
    timeout_seconds: int


def get_qdrant_config() -> QdrantConfig:
    return QdrantConfig(
        endpoint=settings.qdrant_endpoint,
        api_key=settings.qdrant_api_key,
        collection_name=settings.qdrant_collection_agriculture,
        vector_size=settings.qdrant_vector_size,
        timeout_seconds=settings.qdrant_timeout_seconds,
    )
