import hashlib
import math
import re

from app.services.embedding.base import EmbeddingProvider

_CHINESE_RE = re.compile(r"[\u4e00-\u9fff]")
_WORD_RE = re.compile(r"[a-zA-Z0-9_]+")


class MockEmbeddingProvider(EmbeddingProvider):
    """
    离线演示用确定性向量：基于字符 / 词 n-gram 的哈希特征。

    - 不依赖外部模型或密钥，同一文本的向量稳定可复现
    - 文本重叠越多，余弦相似度越高，因此没有真实 embedding 服务时
      仍能支撑检索链路联调与演示（原实现为纯随机哈希，相关性接近 0）
    """

    def __init__(self, vector_size: int):
        self.vector_size = max(16, int(vector_size))

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        return [self._embed_single(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed_single(text)

    @staticmethod
    def _tokenize(text: str) -> list[str]:
        text = (text or "").strip().lower()
        if not text:
            return []
        tokens = _WORD_RE.findall(text)
        chinese_chars = _CHINESE_RE.findall(text)
        if chinese_chars:
            # 中文按「单字 + 相邻双字」切分，既保留词义又避免分词依赖
            tokens.extend(chinese_chars)
            tokens.extend(
                "".join(pair) for pair in zip(chinese_chars, chinese_chars[1:])
            )
        return tokens

    def _embed_single(self, text: str) -> list[float]:
        vector = [0.0] * self.vector_size
        tokens = self._tokenize(text)
        if not tokens:
            tokens = [text or "empty"]

        for token in tokens:
            digest = hashlib.md5(token.encode("utf-8")).digest()
            index = int.from_bytes(digest[:4], "little") % self.vector_size
            sign = 1.0 if digest[4] & 1 else -1.0
            vector[index] += sign

        norm = math.sqrt(sum(value * value for value in vector))
        if norm == 0:
            vector[0] = 1.0
            return vector
        return [value / norm for value in vector]
