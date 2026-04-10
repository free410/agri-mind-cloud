from qdrant_client.http import models

from app.core.qdrant_client import get_qdrant_client
from app.core.qdrant_config import get_qdrant_config
from app.schemas.qdrant import (
    AgricultureMetadataFilter,
    QdrantFilterSearchRequest,
    QdrantInitCollectionResponse,
    QdrantSearchItem,
    QdrantSearchRequest,
    QdrantSearchResponse,
    QdrantUpsertRequest,
    QdrantUpsertResponse,
)


class QdrantService:
    def __init__(self):
        self.client = get_qdrant_client()
        self.config = get_qdrant_config()

    @property
    def collection_name(self) -> str:
        return self.config.collection_name

    def init_collection(self, recreate: bool = False) -> QdrantInitCollectionResponse:
        exists = self.client.collection_exists(self.collection_name)
        recreated = False

        if exists and recreate:
            self.client.delete_collection(self.collection_name)
            exists = False
            recreated = True

        if not exists:
            self.client.create_collection(
                collection_name=self.collection_name,
                vectors_config=models.VectorParams(
                    size=self.config.vector_size,
                    distance=models.Distance.COSINE,
                ),
            )

        self._ensure_metadata_indexes()
        return QdrantInitCollectionResponse(
            collection_name=self.collection_name,
            vector_size=self.config.vector_size,
            distance="COSINE",
            recreated=recreated,
        )

    def upsert_documents(self, request: QdrantUpsertRequest) -> QdrantUpsertResponse:
        points: list[models.PointStruct] = []
        for item in request.items:
            if len(item.embedding) != self.config.vector_size:
                raise ValueError(
                    f"向量维度不匹配：{item.id} 维度为 {len(item.embedding)}，"
                    f"但集合配置是 {self.config.vector_size}"
                )

            metadata = item.metadata.model_dump(exclude_none=True)
            payload = {"text": item.text, "metadata": metadata}
            points.append(models.PointStruct(id=item.id, vector=item.embedding, payload=payload))

        self.client.upsert(collection_name=self.collection_name, points=points, wait=True)
        return QdrantUpsertResponse(
            collection_name=self.collection_name,
            upserted_count=len(points),
        )

    def similarity_search(self, request: QdrantSearchRequest) -> QdrantSearchResponse:
        filter_obj = self._build_metadata_filter(request.filter)
        # qdrant-client>=1.7 使用 query_points；旧版仍提供 search
        if hasattr(self.client, "query_points"):
            response = self.client.query_points(
                collection_name=self.collection_name,
                query=request.query_embedding,
                query_filter=filter_obj,
                limit=request.top_k,
                with_payload=True,
            )
            return self._to_search_response(response.points)
        result = self.client.search(
            collection_name=self.collection_name,
            query_vector=request.query_embedding,
            query_filter=filter_obj,
            limit=request.top_k,
            with_payload=True,
        )
        return self._to_search_response(result)

    def filter_search(self, request: QdrantFilterSearchRequest) -> QdrantSearchResponse:
        base_request = QdrantSearchRequest(
            query_embedding=request.query_embedding,
            top_k=request.top_k,
            filter=request.metadata_filter,
        )
        return self.similarity_search(base_request)

    def _to_search_response(self, result: list[models.ScoredPoint]) -> QdrantSearchResponse:
        items: list[QdrantSearchItem] = []
        for point in result:
            payload = point.payload or {}
            items.append(
                QdrantSearchItem(
                    id=str(point.id),
                    score=float(point.score or 0.0),
                    text=str(payload.get("text", "")),
                    metadata=dict(payload.get("metadata", {}) or {}),
                )
            )

        return QdrantSearchResponse(
            collection_name=self.collection_name,
            total=len(items),
            items=items,
        )

    def _build_metadata_filter(
        self, metadata_filter: AgricultureMetadataFilter | None
    ) -> models.Filter | None:
        if metadata_filter is None:
            return None

        raw_filter = metadata_filter.model_dump(exclude_none=True)
        if not raw_filter:
            return None

        conditions: list[models.FieldCondition] = []
        for key, value in raw_filter.items():
            field_key = f"metadata.{key}"
            if isinstance(value, list):
                conditions.append(
                    models.FieldCondition(
                        key=field_key,
                        match=models.MatchAny(any=value),
                    )
                )
            else:
                conditions.append(
                    models.FieldCondition(
                        key=field_key,
                        match=models.MatchValue(value=value),
                    )
                )

        return models.Filter(must=conditions)

    def _ensure_metadata_indexes(self):
        keyword_fields = [
            "metadata.source_type",
            "metadata.topic",
            "metadata.crop_type",
            "metadata.device_type",
            "metadata.warning_type",
            "metadata.title",
            "metadata.source_name",
        ]
        for field in keyword_fields:
            try:
                self.client.create_payload_index(
                    collection_name=self.collection_name,
                    field_name=field,
                    field_schema=models.PayloadSchemaType.KEYWORD,
                    wait=True,
                )
            except Exception:
                # 索引已存在或当前版本不支持重复创建时，忽略并继续。
                continue


qdrant_service = QdrantService()
