"""
Qdrant 冒烟测试脚本。

运行方式：
python -m app.scripts.qdrant_smoke_test
"""

from app.services.qdrant_service import qdrant_service
from app.schemas.qdrant import (
    AgricultureMetadata,
    AgricultureMetadataFilter,
    QdrantFilterSearchRequest,
    QdrantUpsertRequest,
    QdrantPointUpsertItem,
    QdrantSearchRequest,
)


def build_vector(size: int, seed: float) -> list[float]:
    return [seed for _ in range(size)]


def run():
    init_result = qdrant_service.init_collection(recreate=False)
    vector_size = init_result.vector_size

    upsert_result = qdrant_service.upsert_documents(
        QdrantUpsertRequest(
            items=[
                QdrantPointUpsertItem(
                    id="doc-001",
                    text="土壤湿度偏低时建议先核验灌溉设备状态并逐步补水。",
                    embedding=build_vector(vector_size, 0.1),
                    metadata=AgricultureMetadata(
                        source_type="sop",
                        topic="humidity",
                        crop_type="番茄",
                        device_type="灌溉设备",
                        warning_type="土壤湿度过低",
                        title="土壤湿度处置建议",
                        source_name="农业SOP手册",
                    ),
                ),
                QdrantPointUpsertItem(
                    id="doc-002",
                    text="设备离线预警需要优先检查供电、网络连接和控制器状态。",
                    embedding=build_vector(vector_size, 0.2),
                    metadata=AgricultureMetadata(
                        source_type="device_doc",
                        topic="device",
                        crop_type="草莓",
                        device_type="环境传感器",
                        warning_type="设备离线",
                        title="设备离线排查",
                        source_name="设备运维文档",
                    ),
                ),
            ]
        )
    )

    similarity_result = qdrant_service.similarity_search(
        QdrantSearchRequest(query_embedding=build_vector(vector_size, 0.1), top_k=3)
    )

    filter_result = qdrant_service.filter_search(
        QdrantFilterSearchRequest(
            query_embedding=build_vector(vector_size, 0.1),
            top_k=3,
            metadata_filter=AgricultureMetadataFilter(topic="humidity", source_type="sop"),
        )
    )

    print("[init]", init_result.model_dump())
    print("[upsert]", upsert_result.model_dump())
    print("[search]", similarity_result.model_dump())
    print("[filter]", filter_result.model_dump())


if __name__ == "__main__":
    run()
