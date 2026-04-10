import hashlib

from app.services.embedding.base import EmbeddingProvider


class MockEmbeddingProvider(EmbeddingProvider):
    """
    比赛演示用占位 embedding：
    - 不依赖外部模型
    - 可重复、稳定
    - 仅用于流程联调
    """

    def __init__(self, vector_size: int):
        self.vector_size = vector_size

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        return [self._embed_single(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed_single(text)

    def _embed_single(self, text: str) -> list[float]:
        digest = hashlib.sha256(text.encode("utf-8")).digest()
        values: list[float] = []
        for i in range(self.vector_size):
            b = digest[i % len(digest)]
            values.append((b / 255.0) * 2 - 1)
        return values
