from pydantic import BaseModel


class KnowledgeIndexStats(BaseModel):
    root_dir: str
    file_count: int = 0
    document_count: int = 0
    chunk_count: int = 0
    upserted_count: int = 0
    batch_count: int = 0
    dry_run: bool = False
