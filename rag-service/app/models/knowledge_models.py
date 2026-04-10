from pydantic import BaseModel, Field


class RawKnowledgeDocument(BaseModel):
    doc_id: str
    source_path: str
    source_name: str
    source_type: str
    title: str
    text: str
    ext: str
    topic: str | None = None
    crop_type: str | None = None
    device_type: str | None = None
    warning_type: str | None = None


class KnowledgeChunk(BaseModel):
    chunk_id: str
    doc_id: str
    chunk_index: int = Field(ge=1)
    total_chunks: int = Field(ge=1)
    text: str
    source_path: str
    source_name: str
    source_type: str
    title: str
    ext: str
    topic: str | None = None
    crop_type: str | None = None
    device_type: str | None = None
    warning_type: str | None = None
