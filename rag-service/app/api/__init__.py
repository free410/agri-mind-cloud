from fastapi import APIRouter

from app.api.routes.graph import router as graph_router
from app.api.routes.health import router as health_router
from app.api.routes.knowledge import router as knowledge_router
from app.api.routes.qdrant import router as qdrant_router
from app.api.routes.rag import router as rag_router

router = APIRouter()
router.include_router(health_router, tags=["health"])
router.include_router(rag_router, tags=["rag"])
router.include_router(qdrant_router, tags=["qdrant"])
router.include_router(graph_router, tags=["knowledge-graph"])
router.include_router(knowledge_router)
