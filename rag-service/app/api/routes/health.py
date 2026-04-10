from datetime import datetime

from fastapi import APIRouter

from app.core.response import success

router = APIRouter()


@router.get("/health")
def health_check():
    return success(
        data={
            "status": "UP",
            "service": "rag-service",
            "time": datetime.now().isoformat(timespec="seconds")
        },
        message="RAG service is running"
    )

