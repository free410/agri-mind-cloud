from pydantic import BaseModel, Field


class RetrievedChunk(BaseModel):
    """检索文档分片内部模型（供服务层组合使用）。"""

    id: str
    source: str
    title: str
    content: str
    score: float = 0.0
    channel: str = Field(default="knowledge")
