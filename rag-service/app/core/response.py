from datetime import datetime
from typing import Any

from app.schemas.common import ApiResponse


def success(data: Any = None, message: str = "success", code: int = 0) -> dict[str, Any]:
    response = ApiResponse[Any](
        code=code,
        message=message,
        data=data,
        timestamp=datetime.now().isoformat(timespec="seconds"),
    )
    return response.model_dump()
