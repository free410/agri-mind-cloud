from __future__ import annotations

from typing import Any

import httpx

from app.core.config import settings
from app.schemas.rag import BusinessContextPayload


class SpringBusinessContextProvider:
    """
    从 Spring Boot 拉取业务上下文（可选能力）。
    推荐主流程仍使用“Spring Boot 直接透传 business_context”。
    """

    def fetch(self, user_query: str, hint: dict[str, Any] | None = None) -> BusinessContextPayload | None:
        payload = {
            "query": user_query,
            "hint": hint or {},
        }
        try:
            with httpx.Client(timeout=settings.spring_context_timeout_seconds) as client:
                resp = client.post(settings.spring_context_url, json=payload)
                resp.raise_for_status()
                body = resp.json()
        except Exception:
            return None

        data = body.get("data") if isinstance(body, dict) else None
        if not isinstance(data, dict):
            return None

        try:
            return BusinessContextPayload.model_validate(data)
        except Exception:
            return None


business_context_provider = SpringBusinessContextProvider()
