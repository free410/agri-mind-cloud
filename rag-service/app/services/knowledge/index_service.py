from pathlib import Path

from app.core.config import settings
from app.schemas.knowledge_index import KnowledgeIndexStats
from app.schemas.qdrant import QdrantPointUpsertItem, QdrantUpsertRequest
from app.services.embedding.factory import get_embedding_provider
from app.services.knowledge.chunker import KnowledgeChunker
from app.services.knowledge.document_loader import KnowledgeDocumentLoader
from app.services.knowledge.metadata_builder import KnowledgeMetadataBuilder
from app.services.qdrant_service import qdrant_service


class KnowledgeIndexService:
    def __init__(self):
        self.loader = KnowledgeDocumentLoader()
        self.chunker = KnowledgeChunker()
        self.metadata_builder = KnowledgeMetadataBuilder()
        self.embedding_provider = get_embedding_provider()

    def run(
        self,
        root_dir: str | Path | None = None,
        chunk_size: int | None = None,
        overlap: int | None = None,
        batch_size: int = 64,
        recreate_collection: bool = False,
        dry_run: bool = False,
        include_cleaned_jsonl: bool = True,
    ) -> KnowledgeIndexStats:
        target_dir = Path(root_dir or settings.knowledge_root_dir)
        documents = self.loader.load_documents(target_dir)
        if include_cleaned_jsonl:
            cleaned_dir = settings._resolve_path(settings.crawl_data_root_dir) / "cleaned"
            documents.extend(self.loader.load_cleaned_jsonl_dir(cleaned_dir))
        chunks = self.chunker.chunk_documents(
            documents,
            chunk_size=chunk_size or settings.chunk_size,
            overlap=overlap if overlap is not None else settings.chunk_overlap,
        )

        stats = KnowledgeIndexStats(
            root_dir=str(target_dir.resolve()),
            file_count=self._count_files(target_dir),
            document_count=len(documents),
            chunk_count=len(chunks),
            dry_run=dry_run,
        )

        if dry_run:
            return stats

        qdrant_service.init_collection(recreate=recreate_collection)

        for batch in self._chunk_batch(chunks, batch_size=batch_size):
            vectors = self.embedding_provider.embed_texts([item.text for item in batch])
            request_items: list[QdrantPointUpsertItem] = []
            for chunk, vector in zip(batch, vectors, strict=True):
                request_items.append(
                    QdrantPointUpsertItem(
                        id=chunk.chunk_id,
                        text=chunk.text,
                        embedding=vector,
                        metadata=self.metadata_builder.build(chunk),
                    )
                )

            upsert_result = qdrant_service.upsert_documents(QdrantUpsertRequest(items=request_items))
            stats.upserted_count += upsert_result.upserted_count
            stats.batch_count += 1

        return stats

    @staticmethod
    def _chunk_batch(items: list, batch_size: int):
        for idx in range(0, len(items), batch_size):
            yield items[idx : idx + batch_size]

    @staticmethod
    def _count_files(root_dir: Path) -> int:
        if not root_dir.exists():
            return 0
        return sum(1 for path in root_dir.rglob("*") if path.is_file())


knowledge_index_service = KnowledgeIndexService()
