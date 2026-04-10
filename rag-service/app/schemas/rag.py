import logging
from datetime import datetime
from typing import Annotated, Any, Literal

from pydantic import BaseModel, BeforeValidator, ConfigDict, Field, ValidationError, model_validator

logger = logging.getLogger(__name__)


class RagSearchRequest(BaseModel):
    query: str = Field(min_length=1, max_length=1000, description="检索问题")
    top_k: int = Field(default=5, ge=1, le=20)
    channels: list[Literal["knowledge", "business"]] = Field(
        default_factory=lambda: ["knowledge", "business"]
    )
    context: dict[str, Any] = Field(default_factory=dict)


class RagReferenceItem(BaseModel):
    id: str
    channel: Literal["knowledge", "business"]
    source: str
    title: str
    content: str
    score: float = 0.0
    metadata: dict[str, Any] = Field(default_factory=dict)


class RagSearchResponse(BaseModel):
    query: str
    total: int
    items: list[RagReferenceItem] = Field(default_factory=list)


class RetrievedKnowledgeChunk(BaseModel):
    id: str
    title: str
    content: str
    source: str = ""
    score: float = 0.0
    metadata: dict[str, Any] = Field(default_factory=dict)


class WarningContextItem(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    warning_no: str | None = None
    warning_type: str | None = None
    level: str | None = None
    status: str | None = None
    content: str | None = None
    trigger_time: str | None = None
    farmland_id: int | None = None
    related_device_id: int | None = None
    related_crop_id: int | None = None


class EnvDataContextItem(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    farmland_id: int | None = None
    device_id: int | None = None
    temperature: float | None = None
    humidity: float | None = None
    soil_moisture: float | None = None
    light_intensity: float | None = None
    ph_value: float | None = None
    collect_time: str | None = None


class DeviceContextItem(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    device_code: str | None = None
    device_name: str | None = None
    device_type: str | None = None
    online_status: int | None = None
    run_status: int | None = None
    farmland_id: int | None = None


class FarmlandContextItem(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    field_name: str | None = None
    location: str | None = None
    soil_type: str | None = None
    status: int | None = None


class CropContextItem(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    crop_name: str | None = None
    crop_type: str | None = None
    growth_stage: str | None = None
    health_status: str | None = None
    farmland_id: int | None = None


class WarningProcessLogContextItem(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    warning_id: int | None = None
    action: str | None = None
    operator_name: str | None = None
    operation_time: str | None = None
    remark: str | None = None
    from_status: str | None = None
    to_status: str | None = None


class WarningAiAnalysisContextItem(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    warning_id: int | None = None
    summary: str | None = None
    suggestions: str | None = None
    # Java 端存的是 priorityLevel 整型，与 str 不一致会导致 422
    priority: int | str | None = None
    need_manual_review: bool | None = None
    created_time: str | None = None


class BusinessContextPayload(BaseModel):
    """与 Java 端 AiRagServiceImpl.buildBusinessContext 对齐；允许未声明字段以免 422。"""

    model_config = ConfigDict(extra="ignore")

    current_warning: WarningContextItem | None = None
    recent_warnings: list[WarningContextItem] = Field(default_factory=list)
    recent_env_data: list[EnvDataContextItem] = Field(default_factory=list)
    related_devices: list[DeviceContextItem] = Field(default_factory=list)
    related_farmland: FarmlandContextItem | None = None
    related_crops: list[CropContextItem] = Field(default_factory=list)
    process_logs: list[WarningProcessLogContextItem] = Field(default_factory=list)
    ai_analyses: list[WarningAiAnalysisContextItem] = Field(default_factory=list)
    extra: dict[str, Any] = Field(default_factory=dict)


def _coerce_business_context(value: Any) -> BusinessContextPayload | None:
    """
    Java 透传的 business_context 字段可能随版本增减；严格校验会导致 FastAPI 422。
    解析失败时降级为 None（仍可走知识库 RAG），并打日志便于排查。
    """
    if value is None:
        return None
    if isinstance(value, BusinessContextPayload):
        return value
    if isinstance(value, dict):
        try:
            return BusinessContextPayload.model_validate(value)
        except ValidationError as exc:
            logger.warning(
                "business_context 校验未通过，已忽略该字段以避免 422。errors=%s",
                exc.errors(),
            )
            return None
    logger.warning("business_context 类型异常: %s", type(value).__name__)
    return None


class BuiltBusinessContext(BaseModel):
    question_type: Literal["warning", "knowledge", "hybrid"]
    has_business_data: bool = False
    context_text: str = ""
    risk_signals: list[str] = Field(default_factory=list)
    summary_blocks: list[str] = Field(default_factory=list)
    raw: BusinessContextPayload | None = None


class QueryPlan(BaseModel):
    question_type: Literal["warning", "knowledge", "hybrid"]
    mode: Literal["auto", "business", "knowledge", "hybrid"]
    use_business_context: bool = True
    use_knowledge_retrieval: bool = True
    knowledge_top_k: int = 5
    topic_hint: str | None = None


class RagAnswerRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    # 推荐字段
    query: str | None = Field(default=None, max_length=1500)
    mode: Literal["auto", "business", "knowledge", "hybrid"] = "auto"
    business_context: Annotated[
        BusinessContextPayload | None,
        BeforeValidator(_coerce_business_context),
    ] = None
    related_warning_id: int | None = None
    related_device_id: int | None = None
    related_farmland_id: int | None = None
    retrieved_knowledge_chunks: list[RetrievedKnowledgeChunk] = Field(default_factory=list)

    # 兼容历史字段
    user_query: str | None = Field(default=None, max_length=1500)
    question: str | None = Field(default=None, max_length=1500)
    top_k: int = Field(default=10, ge=1, le=20)
    knowledge_graph_context: str | None = Field(default=None, max_length=12000)
    session_id: str | None = None
    pull_business_context: bool = False
    business_context_hint: dict[str, Any] = Field(default_factory=dict)
    need_structured: bool = True

    @model_validator(mode="after")
    def normalize(self):
        if not self.query:
            self.query = self.user_query or self.question
        if not self.query:
            raise ValueError("query 不能为空")
        self.user_query = self.query
        self.question = self.query
        return self


class ModelStructuredOutput(BaseModel):
    answer_summary: str
    conclusion: str
    possible_causes: list[str] = Field(default_factory=list)
    recommendations: list[str] = Field(default_factory=list)
    priority: Literal["低", "中", "高"] = "中"
    should_manual_review: bool = False


class BusinessCitation(BaseModel):
    summary_blocks: list[str] = Field(default_factory=list)
    risk_signals: list[str] = Field(default_factory=list)
    related_ids: dict[str, int | None] = Field(default_factory=dict)


class KnowledgeCitation(BaseModel):
    id: str
    title: str
    source: str
    score: float
    topic: str | None = None


class RagAnswerResponse(BaseModel):
    session_id: str
    mode: str
    question_type: Literal["warning", "knowledge", "hybrid"]
    answer_summary: str
    conclusion: str
    possible_causes: list[str] = Field(default_factory=list)
    recommendations: list[str] = Field(default_factory=list)
    priority: Literal["低", "中", "高"] = "中"
    should_manual_review: bool = False
    cited_business_context: BusinessCitation | None = None
    cited_knowledge_sources: list[KnowledgeCitation] = Field(default_factory=list)

    # 保留调试与扩展字段
    debug_prompt: str | None = None
    references: list[RagReferenceItem] = Field(default_factory=list)
    business_context: BuiltBusinessContext | None = None


class RagBriefingRequest(BaseModel):
    briefing_type: Literal["daily_exception", "warning_disposal", "closure_summary"] = "daily_exception"
    date: str | None = None
    focus: str | None = None
    top_k: int = Field(default=5, ge=1, le=20)


class RagBriefingResponse(BaseModel):
    title: str
    date: str
    summary: str
    key_points: list[str] = Field(default_factory=list)
    actions: list[str] = Field(default_factory=list)
    references: list[RagReferenceItem] = Field(default_factory=list)


def today_str() -> str:
    return datetime.now().strftime("%Y-%m-%d")
