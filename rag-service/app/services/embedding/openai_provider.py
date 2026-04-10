from __future__ import annotations

import httpx

from app.services.embedding.base import EmbeddingProvider


class OpenAIEmbeddingProvider(EmbeddingProvider):
    """
    OpenAI 兼容 Embeddings（/v1/embeddings），适用于：
    - OpenAI 官方
    - 阿里云百炼 Model Studio 兼容模式（base_url 指向 dashscope compatible-mode/v1）
    """

    def __init__(
        self,
        model: str,
        api_key: str,
        vector_size: int,
        base_url: str,
        dimensions: int | None = None,
        timeout_seconds: float = 120.0,
    ):
        self.model = model
        self.api_key = api_key.strip()
        self.vector_size = vector_size
        self.base_url = base_url.rstrip("/")
        self.dimensions = dimensions
        self.timeout_seconds = timeout_seconds

    def _post_embeddings(self, inputs: list[str]) -> list[list[float]]:
        if not self.api_key:
            raise ValueError("未配置 OPENAI_API_KEY（百炼即 DashScope API Key）")

        url = f"{self.base_url}/embeddings"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        body: dict = {
            "model": self.model,
            "input": inputs,
            "encoding_format": "float",
        }
        if self.dimensions is not None:
            body["dimensions"] = self.dimensions

        with httpx.Client(timeout=self.timeout_seconds) as client:
            resp = client.post(url, json=body, headers=headers)
            resp.raise_for_status()
            data = resp.json()

        err = data.get("error")
        if err:
            raise RuntimeError(err.get("message") or str(err))

        rows = data.get("data") or []
        rows.sort(key=lambda x: x.get("index", 0))
        vectors: list[list[float]] = []
        for row in rows:
            emb = row.get("embedding")
            if not isinstance(emb, list):
                continue
            vectors.append([float(x) for x in emb])
        if len(vectors) != len(inputs):
            raise RuntimeError(f"Embedding 返回条数异常：期望 {len(inputs)}，实际 {len(vectors)}")

        for i, vec in enumerate(vectors):
            if len(vec) != self.vector_size:
                raise ValueError(
                    f"向量维度为 {len(vec)}，与 QDRANT_VECTOR_SIZE={self.vector_size} 不一致。"
                    "请调整 EMBEDDING_DIMENSIONS 与 QDRANT_VECTOR_SIZE，并必要时执行 index_knowledge.py --recreate。"
                )
        return vectors

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        out: list[list[float]] = []
        batch_size = 16
        for i in range(0, len(texts), batch_size):
            batch = texts[i : i + batch_size]
            out.extend(self._post_embeddings(batch))
        return out

    def embed_query(self, text: str) -> list[float]:
        return self._post_embeddings([text])[0]
