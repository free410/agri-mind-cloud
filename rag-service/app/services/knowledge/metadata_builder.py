from datetime import datetime

from app.models.knowledge_models import KnowledgeChunk
from app.schemas.qdrant import AgricultureMetadata


class KnowledgeMetadataBuilder:
    def build(self, chunk: KnowledgeChunk) -> AgricultureMetadata:
        return AgricultureMetadata(
            source_type=chunk.source_type,  # type: ignore[arg-type]
            topic=chunk.topic,  # type: ignore[arg-type]
            crop_type=chunk.crop_type,
            device_type=chunk.device_type,
            warning_type=chunk.warning_type,
            title=chunk.title,
            source_name=chunk.source_name,
            source_path=chunk.source_path,
            file_ext=chunk.ext,
            chunk_index=chunk.chunk_index,
            total_chunks=chunk.total_chunks,
            ingested_at=datetime.now().isoformat(timespec="seconds"),
        )
