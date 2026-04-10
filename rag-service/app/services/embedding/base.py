from abc import ABC, abstractmethod


class EmbeddingProvider(ABC):
    @abstractmethod
    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """批量文本向量化。"""

    @abstractmethod
    def embed_query(self, text: str) -> list[float]:
        """单条查询向量化。"""
