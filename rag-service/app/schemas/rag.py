import logging
from datetime import datetime
from typing import Annotated, Any, Literal, Optional

from pydantic import BaseModel, BeforeValidator, ConfigDict, Field, ValidationError, model_validator

logger = logging.getLogger(__name__)


class RagSearchRequest(BaseModel):
    query: str = Field(min_length=1, max_length=1000, description="检索问题")
    top_k: int = Field(default=5, ge=1, le=20)
    channels: list[Literal["knowledge", "business"]] = Field(
        default_factory=lambda: ["knowledge", "business"]
    )
    context: dict[str, Any] = Field(default_factory=dict)
    # 新增：意图路由参数
    intent_domain: Optional[str] = Field(default=None, description="指定检索的知识领域")
    min_similarity_score: Optional[float] = Field(default=0.5, ge=0.0, le=1.0, description="最低相似度阈值")


class RagReferenceItem(BaseModel):
    id: str
    channel: Literal["knowledge", "business"]
    source: str
    title: str
    content: str
    score: float = 0.0
    metadata: dict[str, Any] = Field(default_factory=dict)
    # 新增：重排序分数
    rerank_score: Optional[float] = None


class RagSearchResponse(BaseModel):
    query: str
    total: int
    items: list[RagReferenceItem] = Field(default_factory=list)
    # 新增：检索元数据
    retrieval_metadata: Optional[dict[str, Any]] = None


class RetrievedKnowledgeChunk(BaseModel):
    id: str
    title: str
    content: str
    source: str = ""
    score: float = 0.0
    metadata: dict[str, Any] = Field(default_factory=dict)
    # 新增：重排序分数和领域标记
    rerank_score: Optional[float] = None
    domain: Optional[str] = None


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
    question_type: Literal["warning", "knowledge", "hybrid"] = "knowledge"
    has_business_data: bool = False
    context_text: str = ""
    risk_signals: list[str] = Field(default_factory=list)
    summary_blocks: list[str] = Field(default_factory=list)
    raw: BusinessContextPayload | None = None


class QueryPlan(BaseModel):
    """RAG查询计划：包含意图路由、检索策略等"""
    question_type: Literal["warning", "knowledge", "hybrid"] = "knowledge"
    mode: Literal["auto", "business", "knowledge", "hybrid"] = "auto"
    use_business_context: bool = True
    use_knowledge_retrieval: bool = True
    knowledge_top_k: int = 5
    topic_hint: str | None = None

    # ========== 新增字段 ==========
    # 意图路由信息
    intent_domain: str = "general"  # crop_disease, market_price, agri_policy, general
    intent_confidence: float = 0.5  # 0.0 ~ 1.0
    matched_keywords: list[str] = Field(default_factory=list)

    # 检索策略
    target_collections: list[str] = Field(default_factory=list)  # 目标Qdrant集合列表
    min_similarity_score: float = 0.5  # 最低相似度阈值


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

    # 图谱引用：包含 entity_id, entity_name, entity_kind, subgraph
    cited_knowledge_graph: list[dict[str, Any]] = Field(default_factory=list)

    # 兼容历史字段
    user_query: str | None = Field(default=None, max_length=1500)
    question: str | None = Field(default=None, max_length=1500)
    top_k: int = Field(default=10, ge=1, le=20)
    knowledge_graph_context: str | None = Field(default=None, max_length=12000)
    session_id: str | None = None
    pull_business_context: bool = False
    business_context_hint: dict[str, Any] = Field(default_factory=dict)
    need_structured: bool = True

    # ========== 新增字段 ==========
    # 意图路由参数（用于覆盖自动判断）
    intent_domain: str | None = Field(default=None, description="手动指定知识领域")
    # 检索阈值（用于覆盖默认阈值）
    min_similarity_score: float | None = Field(default=None, ge=0.0, le=1.0)

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
    answer_summary: str = ""
    conclusion: str = ""
    possible_causes: list[str] = Field(default_factory=list)
    recommendations: list[str] = Field(default_factory=list)
    priority: Literal["低", "中", "高"] = "中"
    should_manual_review: bool = False
    # LLM 兜底时说明原因，前端据此显示提示
    fallback_reason: str | None = Field(default=None, alias="fallbackReason")
    # 新增：检索有效性标记
    has_valid_retrieval: bool = True  # 是否有有效的检索结果


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
    # 新增：重排序分数
    rerank_score: float | None = None
    # 新增：领域标记
    domain: str | None = None
    # 新增：完整内容（用于前端检索详情展示）
    content: str | None = None
    # 新增：元数据
    metadata: dict[str, Any] = Field(default_factory=dict)


class RagAnswerResponse(BaseModel):
    session_id: str = "unknown"
    mode: str = "auto"
    question_type: Literal["warning", "knowledge", "hybrid"] = "knowledge"
    answer_summary: str = ""
    conclusion: str = ""
    possible_causes: list[str] = Field(default_factory=list)
    recommendations: list[str] = Field(default_factory=list)
    priority: Literal["低", "中", "高"] = "中"
    should_manual_review: bool = False
    cited_business_context: BusinessCitation | None = None
    cited_knowledge_sources: list[KnowledgeCitation] = Field(default_factory=list)

    # 知识图谱引用（实体 + 子图），由 rag_service.answer() 自动提取
    cited_knowledge_graph: list[dict[str, Any]] = Field(default_factory=list)

    # LLM 兜底原因，用于前端提示
    fallback_reason: str | None = None

    # 保留调试与扩展字段
    debug_prompt: str | None = None
    references: list[RagReferenceItem] = Field(default_factory=list)
    business_context: BuiltBusinessContext | None = None

    # ========== 新增字段 ==========
    # 意图路由信息（便于前端展示）
    intent_domain: str | None = None
    intent_confidence: float | None = None
    matched_keywords: list[str] = Field(default_factory=list)
    # 检索元数据
    retrieval_metadata: dict[str, Any] = Field(default_factory=dict)
    # 无有效检索时标记
    has_valid_retrieval: bool = True
    # 反馈ID（用于用户反馈埋点）
    feedback_id: str | None = None


class RagBriefingRequest(BaseModel):
    briefing_type: Literal["daily_exception", "warning_disposal", "closure_summary"] = "daily_exception"
    date: str | None = None
    focus: str | None = None
    top_k: int = Field(default=5, ge=1, le=20)


class RagBriefingResponse(BaseModel):
    title: str = ""
    date: str = ""
    summary: str = ""
    key_points: list[str] = Field(default_factory=list)
    actions: list[str] = Field(default_factory=list)
    references: list[RagReferenceItem] = Field(default_factory=list)


class UserFeedbackRequest(BaseModel):
    """用户反馈请求：用于「有用/无用」反馈埋点"""
    session_id: str = Field(..., description="会话ID")
    question: str = Field(..., description="用户问题")
    answer_id: str | None = Field(default=None, description="答案ID")
    is_helpful: bool = Field(..., description="是否有用")
    feedback_text: str | None = Field(default=None, max_length=500, description="反馈详情")
    retrieved_refs: list[str] = Field(default_factory=list, description="本次检索引用的文档ID列表")


class UserFeedbackResponse(BaseModel):
    """用户反馈响应"""
    success: bool
    message: str = ""
    feedback_id: str = ""


def today_str() -> str:
    return datetime.now().strftime("%Y-%m-%d")