import uuid

from app.models.knowledge_models import KnowledgeChunk, RawKnowledgeDocument

# 确定性 UUID，满足 Qdrant 对 point id「整数或 UUID」的要求
_CHUNK_ID_NAMESPACE = uuid.UUID("8c5b2f0e-6d1a-4f3c-9e7b-2a4d6c8e0f1a")


class KnowledgeChunker:
    def chunk_documents(
        self,
        documents: list[RawKnowledgeDocument],
        chunk_size: int = 500,
        overlap: int = 80,
    ) -> list[KnowledgeChunk]:
        if chunk_size <= 0:
            raise ValueError("chunk_size 必须大于 0")
        if overlap < 0:
            raise ValueError("overlap 不能小于 0")
        if overlap >= chunk_size:
            raise ValueError("overlap 必须小于 chunk_size")

        chunks: list[KnowledgeChunk] = []
        for doc in documents:
            chunk_texts = self._split_text(doc.text, chunk_size=chunk_size, overlap=overlap)
            total = len(chunk_texts)
            for idx, text in enumerate(chunk_texts, start=1):
                chunk_id = str(
                    uuid.uuid5(_CHUNK_ID_NAMESPACE, f"{doc.doc_id}:{idx}")
                )
                chunks.append(
                    KnowledgeChunk(
                        chunk_id=chunk_id,
                        doc_id=doc.doc_id,
                        chunk_index=idx,
                        total_chunks=total,
                        text=text,
                        source_path=doc.source_path,
                        source_name=doc.source_name,
                        source_type=doc.source_type,
                        title=doc.title,
                        ext=doc.ext,
                        topic=doc.topic,
                        crop_type=doc.crop_type,
                        device_type=doc.device_type,
                        warning_type=doc.warning_type,
                    )
                )
        return chunks

    @staticmethod
    def _split_text(text: str, chunk_size: int, overlap: int) -> list[str]:
        normalized = text.strip().replace("\r\n", "\n")
        if len(normalized) <= chunk_size:
            return [normalized]

        chunks: list[str] = []
        start = 0
        step = chunk_size - overlap

        while start < len(normalized):
            end = min(start + chunk_size, len(normalized))
            chunk = normalized[start:end].strip()
            if chunk:
                chunks.append(chunk)
            if end >= len(normalized):
                break
            start += step
        return chunks
