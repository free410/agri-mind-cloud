import logging
from datetime import datetime

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api import router as api_router
from app.core.config import settings
from app.services.qdrant_service import qdrant_service

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        description="智慧农业 AI / RAG 服务层（FastAPI + Qdrant）",
    )

    @app.on_event("startup")
    async def init_vector_collections() -> None:
        # 启动时确保所有向量子库都存在，避免意图路由到未创建的集合时检索报错
        try:
            created = qdrant_service.init_all_collections()
            logger.info("Qdrant 集合初始化完成，共 %d 个", len(created))
        except Exception as exc:  # Qdrant 暂不可用时不阻塞服务启动
            logger.warning("Qdrant 集合初始化失败: %s", exc)

    @app.exception_handler(RequestValidationError)
    async def request_validation_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
        logger.warning("请求体验证失败(422): %s", exc.errors())
        return JSONResponse(status_code=422, content={"detail": exc.errors()})

    @app.exception_handler(ValueError)
    async def value_error_handler(_: Request, exc: ValueError) -> JSONResponse:
        return JSONResponse(
            status_code=400,
            content={
                "code": 400,
                "message": str(exc),
                "data": None,
                "timestamp": datetime.now().isoformat(timespec="seconds"),
            },
        )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(api_router)
    return app


app = create_app()
