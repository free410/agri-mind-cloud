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
        self._collection_configs = self._get_collection_configs()

    def _get_collection_configs(self) -> dict:
        """获取所有向量子库的配置"""
        return {
            "agriculture_knowledge": {
                "vector_size": self.config.vector_size,
                "description": "通用农业知识库（兜底）",
                "domains": ["general", "unknown"],
            },
            "agriculture_crop_disease": {
                "vector_size": self.config.vector_size,
                "description": "作物病害与种植技术知识库",
                "domains": ["crop_disease"],
            },
            "agriculture_market_price": {
                "vector_size": self.config.vector_size,
                "description": "农产品价格与市场知识库",
                "domains": ["market_price"],
            },
            "agriculture_agri_policy": {
                "vector_size": self.config.vector_size,
                "description": "农业政策与补贴知识库",
                "domains": ["agri_policy"],
            },
        }

    @property
    def collection_name(self) -> str:
        return self.config.collection_name

    def init_collection(self, recreate: bool = False) -> QdrantInitCollectionResponse:
        """初始化默认集合"""
        return self.init_named_collection(self.collection_name, recreate)

    def init_named_collection(
        self, collection_name: str, recreate: bool = False
    ) -> QdrantInitCollectionResponse:
        """
        初始化指定名称的集合。

        Args:
            collection_name: 集合名称
            recreate: 是否删除重建

        Returns:
            初始化结果
        """
        exists = self.client.collection_exists(collection_name)
        recreated = False

        if exists and recreate:
            self.client.delete_collection(collection_name)
            exists = False
            recreated = True

        # 先解析向量维度：即使集合已存在，返回值也需要它
        vector_size = self.config.vector_size
        if collection_name in self._collection_configs:
            vector_size = self._collection_configs[collection_name].get("vector_size", vector_size)

        if not exists:
            self.client.create_collection(
                collection_name=collection_name,
                vectors_config=models.VectorParams(
                    size=vector_size,
                    distance=models.Distance.COSINE,
                ),
            )

        self._ensure_metadata_indexes(collection_name)
        return QdrantInitCollectionResponse(
            collection_name=collection_name,
            vector_size=vector_size,
            distance="COSINE",
            recreated=recreated,
        )

    def init_all_collections(self, recreate: bool = False) -> list[QdrantInitCollectionResponse]:
        """初始化所有配置的集合"""
        results = []
        for collection_name in self._collection_configs.keys():
            try:
                result = self.init_named_collection(collection_name, recreate)
                results.append(result)
            except Exception as e:
                # 某些集合可能已存在但无法重建
                import logging
                logging.warning(f"初始化集合 {collection_name} 失败: {e}")
        return results

    def _source_filter(self, source_path: str) -> models.Filter:
        return models.Filter(
            must=[
                models.FieldCondition(
                    key="metadata.source_path",
                    match=models.MatchValue(value=source_path),
                )
            ]
        )

    def count_points_by_source(self, source_path: str) -> int:
        """统计某个知识文档在所有子库中的向量条数"""
        total = 0
        source_filter = self._source_filter(source_path)
        for name in self._collection_configs:
            if not self.client.collection_exists(name):
                continue
            result = self.client.count(
                collection_name=name, count_filter=source_filter, exact=True
            )
            total += result.count
        return total

    def delete_points_by_source(self, source_path: str) -> int:
        """按知识文档路径删除向量，返回被删除的条数"""
        deleted = self.count_points_by_source(source_path)
        if deleted == 0:
            return 0
        selector = models.FilterSelector(filter=self._source_filter(source_path))
        for name in self._collection_configs:
            if not self.client.collection_exists(name):
                continue
            self.client.delete(
                collection_name=name, points_selector=selector, wait=True
            )
        return deleted

    def upsert_documents(self, request: QdrantUpsertRequest) -> QdrantUpsertResponse:
        """向默认集合插入文档"""
        return self.upsert_to_collection(request, self.collection_name)

    def upsert_to_collection(
        self, request: QdrantUpsertRequest, collection_name: str | None = None
    ) -> QdrantUpsertResponse:
        """
        向指定集合插入文档。

        Args:
            request: 插入请求
            collection_name: 目标集合名称，默认使用主集合

        Returns:
            插入结果
        """
        target_collection = collection_name or self.collection_name
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

        self.client.upsert(collection_name=target_collection, points=points, wait=True)
        return QdrantUpsertResponse(
            collection_name=target_collection,
            upserted_count=len(points),
        )

    def similarity_search(self, request: QdrantSearchRequest) -> QdrantSearchResponse:
        """在默认集合中检索"""
        return self.search_collection(
            collection_name=self.collection_name,
            query_embedding=request.query_embedding,
            top_k=request.top_k,
            metadata_filter=request.filter,
        )

    def search_collection(
        self,
        collection_name: str,
        query_embedding: list[float],
        top_k: int = 5,
        metadata_filter: AgricultureMetadataFilter | None = None,
    ) -> QdrantSearchResponse:
        """
        在指定集合中检索。

        Args:
            collection_name: 集合名称
            query_embedding: 查询向量
            top_k: 返回数量
            metadata_filter: 元数据过滤条件

        Returns:
            检索结果
        """
        filter_obj = self._build_metadata_filter(metadata_filter)

        try:
            if hasattr(self.client, "query_points"):
                # 新版本 Qdrant 使用 query_points
                response = self.client.query_points(
                    collection_name=collection_name,
                    query=query_embedding,
                    query_filter=filter_obj,
                    limit=top_k,
                    with_payload=True,
                )
                points = response.points
            else:
                # 旧版本使用 search
                result = self.client.search(
                    collection_name=collection_name,
                    query_vector=query_embedding,
                    query_filter=filter_obj,
                    limit=top_k,
                    with_payload=True,
                )
                points = result
            return self._to_search_response(points, collection_name)
        except Exception as e:
            import logging
            import traceback
            logging.error(f"集合 {collection_name} 检索失败: {e}")
            logging.error(f"详细异常: {traceback.format_exc()}")
            return QdrantSearchResponse(
                collection_name=collection_name,
                total=0,
                items=[],
            )

    def keyword_search(
        self,
        collection_name: str,
        keywords: list[str],
        top_k: int = 10,
    ) -> QdrantSearchResponse:
        """
        【新增】基于关键词的全文检索兜底。

        当向量检索效果不好时，使用关键词匹配来找相关文档。
        通过扫描 payload 中的 text 和 title 字段进行匹配。

        Args:
            collection_name: 集合名称
            keywords: 关键词列表
            top_k: 返回数量

        Returns:
            基于关键词匹配的检索结果（按匹配度降序）
        """
        try:
            # 读取集合中的所有文档（Qdrant 的 scroll API）
            all_points = self.client.scroll(
                collection_name=collection_name,
                limit=1000,
                with_payload=True,
                with_vectors=False,
            )[0]

            scored_items: list[tuple[float, models.PointStruct]] = []
            for point in all_points:
                payload = point.payload or {}
                text = str(payload.get("text", "")).lower()
                metadata = payload.get("metadata", {}) or {}
                title = str(metadata.get("title", "")).lower()

                # 计算关键词匹配度
                match_count = 0
                matched_keywords = []
                for kw in keywords:
                    kw_lower = kw.lower()
                    if kw_lower in text or kw_lower in title:
                        match_count += 1
                        matched_keywords.append(kw)

                if match_count > 0:
                    # 匹配度 = 匹配的关键词数 / 总关键词数
                    score = match_count / len(keywords)
                    # 标题匹配权重更高
                    for kw in keywords:
                        if kw.lower() in title:
                            score += 0.2
                    scored_items.append((score, point))

            # 按匹配度降序排序
            scored_items.sort(key=lambda x: x[0], reverse=True)

            # 取 top_k
            top_items = scored_items[:top_k]

            items: list[QdrantSearchItem] = []
            for score, point in top_items:
                payload = point.payload or {}
                item = QdrantSearchItem(
                    id=str(point.id),
                    score=float(score),
                    text=str(payload.get("text", "")),
                    metadata=dict(payload.get("metadata", {}) or {}),
                )
                item.metadata["_collection"] = collection_name
                item.metadata["_keyword_match"] = True
                items.append(item)

            return QdrantSearchResponse(
                collection_name=collection_name,
                total=len(items),
                items=items,
            )

        except Exception as e:
            import logging
            logging.error(f"关键词检索失败 collection={collection_name}: {e}")
            return QdrantSearchResponse(
                collection_name=collection_name,
                total=0,
                items=[],
            )

    def multi_collection_search(
        self,
        query_embedding: list[float],
        collection_names: list[str],
        top_k_per_collection: int = 5,
        metadata_filter: AgricultureMetadataFilter | None = None,
    ) -> list[QdrantSearchItem]:
        """
        在多个集合中搜索并合并结果。

        Args:
            query_embedding: 查询向量
            collection_names: 要搜索的集合列表
            top_k_per_collection: 每个集合返回的数量
            metadata_filter: 元数据过滤

        Returns:
            合并后的检索结果（按相似度降序）
        """
        all_items: list[QdrantSearchItem] = []

        for collection_name in collection_names:
            result = self.search_collection(
                collection_name=collection_name,
                query_embedding=query_embedding,
                top_k=top_k_per_collection,
                metadata_filter=metadata_filter,
            )
            all_items.extend(result.items)

        # 按相似度降序排序
        all_items.sort(key=lambda x: x.score, reverse=True)
        return all_items

    def filter_search(self, request: QdrantFilterSearchRequest) -> QdrantSearchResponse:
        """仅元数据过滤检索（不支持向量检索，保留兼容性）"""
        return self.similarity_search(
            QdrantSearchRequest(
                query_embedding=request.query_embedding,
                top_k=request.top_k,
                filter=request.metadata_filter,
            )
        )

    def _to_search_response(
        self, result: list[models.ScoredPoint], collection_name: str | None = None
    ) -> QdrantSearchResponse:
        """将Qdrant结果转换为统一响应格式"""
        items: list[QdrantSearchItem] = []
        for point in result:
            payload = point.payload or {}
            item = QdrantSearchItem(
                id=str(point.id),
                score=float(point.score or 0.0),
                text=str(payload.get("text", "")),
                metadata=dict(payload.get("metadata", {}) or {}),
            )
            # 添加集合名称到元数据
            if collection_name:
                item.metadata["_collection"] = collection_name
            items.append(item)

        return QdrantSearchResponse(
            collection_name=collection_name or self.collection_name,
            total=len(items),
            items=items,
        )

    def _build_metadata_filter(
        self, metadata_filter: AgricultureMetadataFilter | None
    ) -> models.Filter | None:
        """构建Qdrant元数据过滤条件"""
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

    def _ensure_metadata_indexes(self, collection_name: str | None = None):
        """确保元数据索引已创建"""
        target_collection = collection_name or self.collection_name
        keyword_fields = [
            "metadata.source_type",
            "metadata.topic",
            "metadata.crop_type",
            "metadata.device_type",
            "metadata.warning_type",
            "metadata.title",
            "metadata.source_name",
            "metadata.domain",  # 新增领域标记
            "metadata.content_type",  # 内容类型标记
        ]
        for field in keyword_fields:
            try:
                self.client.create_payload_index(
                    collection_name=target_collection,
                    field_name=field,
                    field_schema=models.PayloadSchemaType.KEYWORD,
                    wait=True,
                )
            except Exception:
                # 索引已存在或当前版本不支持重复创建时，忽略并继续。
                continue

    def collection_exists(self, collection_name: str) -> bool:
        """检查集合是否存在"""
        return self.client.collection_exists(collection_name)

    def list_collections(self) -> list[str]:
        """列出所有集合名称"""
        try:
            collections = self.client.get_collections().collections
            return [c.name for c in collections]
        except Exception:
            return []


qdrant_service = QdrantService()