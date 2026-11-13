from __future__ import annotations

import logging

from app.services.embedding.base import EmbeddingProvider

logger = logging.getLogger(__name__)


class LocalEmbeddingProvider(EmbeddingProvider):
    """
    本地 embedding 模型，使用 sentence-transformers。
    支持中文和多语言模型，如 paraphrase-multilingual 系列。

    sentence-transformers 体积较大，默认不随主依赖安装，
    需要本地向量模型时执行：pip install -r requirements-local-embedding.txt
    """

    def __init__(
        self,
        model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
        device: str = "cpu",
        normalize: bool = True,
    ):
        try:
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:
            raise RuntimeError(
                "EMBEDDING_PROVIDER=local 需要 sentence-transformers，"
                "请执行 pip install -r requirements-local-embedding.txt 后重启服务"
            ) from exc

        self.model_name = model_name
        self.device = device
        self.normalize = normalize
        logger.info("Loading local embedding model: %s", model_name)
        self.model = SentenceTransformer(model_name, device=device)
        self._embedding_dim = self.model.get_sentence_embedding_dimension()
        logger.info(
            "Local embedding model loaded: %s, dimension=%d",
            model_name,
            self._embedding_dim,
        )

    @property
    def embedding_dim(self) -> int:
        return self._embedding_dim

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        embeddings = self.model.encode(
            texts,
            batch_size=32,
            show_progress_bar=False,
            normalize_embeddings=self.normalize,
            convert_to_numpy=True,
        )
        return embeddings.tolist()

    def embed_query(self, text: str) -> list[float]:
        embedding = self.model.encode(
            [text],
            normalize_embeddings=self.normalize,
            convert_to_numpy=True,
        )[0]
        return embedding.tolist()