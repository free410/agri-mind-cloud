from typing import Literal

from pydantic import BaseModel, Field, model_validator


class AgricultureMetadata(BaseModel):
    source_type: Literal["manual", "sop", "rule", "crop_guide", "device_doc"] | None = None
    topic: Literal["warning", "irrigation", "humidity", "device", "crop"] | None = None
    crop_type: str | None = None
    device_type: str | None = None
    warning_type: str | None = None
    title: str | None = None
    source_name: str | None = None
    source_path: str | None = None
    file_ext: str | None = None
    chunk_index: int | None = None
    total_chunks: int | None = None
    ingested_at: str | None = None


class QdrantPointUpsertItem(BaseModel):
    id: str
    text: str = Field(min_length=1)
    embedding: list[float] = Field(min_length=1)
    metadata: AgricultureMetadata = Field(default_factory=AgricultureMetadata)


class QdrantInitCollectionRequest(BaseModel):
    recreate: bool = False


class QdrantInitCollectionResponse(BaseModel):
    collection_name: str
    vector_size: int
    distance: str = "COSINE"
    recreated: bool


class QdrantUpsertRequest(BaseModel):
    items: list[QdrantPointUpsertItem] = Field(min_length=1)


class QdrantUpsertResponse(BaseModel):
    collection_name: str
    upserted_count: int


class AgricultureMetadataFilter(BaseModel):
    source_type: str | list[str] | None = None
    topic: str | list[str] | None = None
    crop_type: str | list[str] | None = None
    device_type: str | list[str] | None = None
    warning_type: str | list[str] | None = None
    title: str | list[str] | None = None
    source_name: str | list[str] | None = None


class QdrantSearchRequest(BaseModel):
    query_embedding: list[float] = Field(min_length=1)
    top_k: int = Field(default=5, ge=1, le=30)
    filter: AgricultureMetadataFilter | None = None


class QdrantSearchItem(BaseModel):
    id: str
    score: float
    text: str = ""
    metadata: dict = Field(default_factory=dict)


class QdrantSearchResponse(BaseModel):
    collection_name: str
    total: int
    items: list[QdrantSearchItem] = Field(default_factory=list)


class QdrantFilterSearchRequest(BaseModel):
    query_embedding: list[float] = Field(min_length=1)
    top_k: int = Field(default=5, ge=1, le=30)
    metadata_filter: AgricultureMetadataFilter = Field(default_factory=AgricultureMetadataFilter)

    @model_validator(mode="after")
    def validate_filter_not_empty(self):
        if not self.metadata_filter.model_dump(exclude_none=True):
            raise ValueError("metadata_filter 不能为空")
        return self
