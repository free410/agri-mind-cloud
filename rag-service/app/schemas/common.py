from datetime import datetime
from typing import Any, Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    code: int = 0
    message: str = "success"
    data: T | None = None
    timestamp: str = Field(default_factory=lambda: datetime.now().isoformat(timespec="seconds"))


class BaseMeta(BaseModel):
    source: str | None = None
    score: float | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)

