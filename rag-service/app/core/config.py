from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv
from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

# 固定为 rag-service 根目录下的 .env，避免从其它工作目录启动 uvicorn 时读不到配置
_RAG_SERVICE_ROOT = Path(__file__).resolve().parents[2]
_DOTENV_PATH = _RAG_SERVICE_ROOT / ".env"

# Windows 上若存在空的 NEO4J_PASSWORD 环境变量，pydantic-settings 会优先采信它而忽略 .env，导致图谱一直 503
if _DOTENV_PATH.is_file():
    load_dotenv(_DOTENV_PATH, override=True)


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=_DOTENV_PATH,
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    app_name: str = Field(default="SmartAgri RAG Service", alias="APP_NAME")
    app_env: str = Field(default="dev", alias="APP_ENV")
    app_debug: bool = Field(default=True, alias="APP_DEBUG")
    app_host: str = Field(default="0.0.0.0", alias="APP_HOST")
    app_port: int = Field(default=9001, alias="APP_PORT")

    cors_allow_origins: str = Field(default="http://localhost:5173", alias="CORS_ALLOW_ORIGINS")

    qdrant_host: str = Field(default="localhost", alias="QDRANT_HOST")
    qdrant_port: int = Field(default=6333, alias="QDRANT_PORT")
    qdrant_scheme: str = Field(default="http", alias="QDRANT_SCHEME")
    qdrant_url: str | None = Field(default=None, alias="QDRANT_URL")
    qdrant_api_key: str = Field(default="", alias="QDRANT_API_KEY")
    qdrant_collection_agriculture: str = Field(
        default="agriculture_knowledge",
        alias="QDRANT_COLLECTION_AGRICULTURE"
    )
    qdrant_vector_size: int = Field(default=1536, alias="QDRANT_VECTOR_SIZE")
    qdrant_timeout_seconds: int = Field(default=10, alias="QDRANT_TIMEOUT_SECONDS")

    embedding_model: str = Field(default="text-embedding-v4", alias="EMBEDDING_MODEL")
    embedding_provider: str = Field(default="openai", alias="EMBEDDING_PROVIDER")
    openai_base_url: str = Field(
        default="https://dashscope.aliyuncs.com/compatible-mode/v1",
        alias="OPENAI_BASE_URL",
        description="OpenAI 兼容根路径（默认百炼）",
    )
    openai_api_key: str = Field(default="", alias="OPENAI_API_KEY")
    openai_timeout_seconds: float = Field(default=120.0, alias="OPENAI_TIMEOUT_SECONDS")
    embedding_dimensions: int | None = Field(
        default=1536,
        alias="EMBEDDING_DIMENSIONS",
        description="百炼 v3/v4 可调维度，须与 QDRANT_VECTOR_SIZE 一致；官方 OpenAI 可置空省略该字段",
    )
    llm_provider: str = Field(default="bailian", alias="LLM_PROVIDER")
    llm_model: str = Field(default="qwen3.5-flash", alias="LLM_MODEL")
    llm_temperature: float = Field(default=0.38, alias="LLM_TEMPERATURE")
    llm_max_tokens: int = Field(
        default=3072,
        alias="LLM_MAX_TOKENS",
        description="结构化 JSON 输出上限，适当增大可显著加长 answer_summary / conclusion 与列表条目",
    )
    llm_fallback_to_mock: bool = Field(default=False, alias="LLM_FALLBACK_TO_MOCK")
    knowledge_retrieval_fallback_to_mock: bool = Field(
        default=True,
        alias="KNOWLEDGE_RETRIEVAL_FALLBACK_TO_MOCK",
        description="Qdrant/向量检索异常时回退到本地 mock 片段，本地开发默认真；生产可设 false",
    )

    spring_backend_base_url: str = Field(default="http://localhost:8080/api", alias="SPRING_BACKEND_BASE_URL")
    spring_context_api_path: str = Field(default="/ai/rag/context", alias="SPRING_CONTEXT_API_PATH")
    spring_context_timeout_seconds: int = Field(default=5, alias="SPRING_CONTEXT_TIMEOUT_SECONDS")
    knowledge_root_dir: str = Field(default="knowledge", alias="KNOWLEDGE_ROOT_DIR")
    chunk_size: int = Field(default=500, alias="CHUNK_SIZE")
    chunk_overlap: int = Field(default=80, alias="CHUNK_OVERLAP")
    crawl_enabled: bool = Field(default=True, alias="CRAWL_ENABLED")
    crawl_user_agent: str = Field(
        default="SmartAgriKnowledgeBot/1.0 (+https://localhost/smart-agri-rag)",
        alias="CRAWL_USER_AGENT"
    )
    crawl_request_timeout_seconds: int = Field(default=15, alias="CRAWL_REQUEST_TIMEOUT_SECONDS")
    crawl_request_interval_seconds: float = Field(default=1.0, alias="CRAWL_REQUEST_INTERVAL_SECONDS")
    crawl_min_content_length: int = Field(default=260, alias="CRAWL_MIN_CONTENT_LENGTH")
    crawl_data_root_dir: str = Field(default="data", alias="CRAWL_DATA_ROOT_DIR")
    crawl_sources_config: str = Field(
        default="sources/china_agri_sources.yaml",
        alias="CRAWL_SOURCES_CONFIG"
    )
    crawl_max_articles_per_source: int = Field(default=20, alias="CRAWL_MAX_ARTICLES_PER_SOURCE")

    neo4j_enabled: bool = Field(default=True, alias="NEO4J_ENABLED")
    neo4j_uri: str = Field(default="bolt://localhost:7687", alias="NEO4J_URI")
    neo4j_user: str = Field(default="neo4j", alias="NEO4J_USER")
    neo4j_password: str = Field(default="", alias="NEO4J_PASSWORD")
    neo4j_database: str | None = Field(default=None, alias="NEO4J_DATABASE")
    neo4j_max_subgraph_paths: int = Field(default=400, alias="NEO4J_MAX_SUBGRAPH_PATHS")

    @field_validator("neo4j_password", mode="before")
    @classmethod
    def _strip_neo4j_password(cls, value: object) -> str:
        if value is None:
            return ""
        return str(value).strip()

    @property
    def project_root(self) -> Path:
        return _RAG_SERVICE_ROOT

    def _resolve_path(self, raw_path: str) -> Path:
        path = Path(raw_path)
        if path.is_absolute():
            return path
        return self.project_root / path

    @property
    def cors_origins(self) -> list[str]:
        return [origin.strip() for origin in self.cors_allow_origins.split(",") if origin.strip()]

    @property
    def qdrant_endpoint(self) -> str:
        if self.qdrant_url:
            return self.qdrant_url
        return f"{self.qdrant_scheme}://{self.qdrant_host}:{self.qdrant_port}"

    @property
    def spring_context_url(self) -> str:
        return f"{self.spring_backend_base_url.rstrip('/')}{self.spring_context_api_path}"

    @property
    def crawl_raw_dir(self) -> str:
        return str(self._resolve_path(self.crawl_data_root_dir) / "raw")

    @property
    def crawl_cleaned_dir(self) -> str:
        return str(self._resolve_path(self.crawl_data_root_dir) / "cleaned")

    @property
    def crawl_manifest_path(self) -> str:
        return str(Path(self.crawl_cleaned_dir) / "crawl_manifest.json")

    @property
    def crawl_sources_config_path(self) -> str:
        return str(self._resolve_path(self.crawl_sources_config))


def get_settings() -> Settings:
    return Settings()


settings = get_settings()
