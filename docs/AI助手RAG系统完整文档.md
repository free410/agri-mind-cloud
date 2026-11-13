# 智慧农业 AI 助手 RAG 系统文档

## 一、系统概述

### 1.1 项目背景

智慧农业 AI 助手是基于检索增强生成（RAG）技术的智能问答系统，旨在为农业生产者、农技人员和农业管理者提供精准的农业知识服务。系统整合了：

- **业务数据**：预警信息、设备状态、环境监测数据
- **专业知识**：作物病害、种植技术、施肥管理、病虫害防治
- **知识图谱**：农业概念实体与关系网络

### 1.2 核心能力

| 能力维度 | 具体描述 |
|---------|---------|
| 智能问答 | 结合业务上下文与知识库的专业回答 |
| 预警分析 | 关联预警数据，提供针对性处置建议 |
| 知识检索 | 向量相似度检索，返回可信的知识来源 |
| 多模式支持 | 纯知识模式、业务联动模式、混合分析模式 |
| 可信度评估 | 显示向量相似度，帮助判断回答可靠性 |

---

## 二、系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端 (Vue 3)                                    │
│                        AIAssistant.vue                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼ HTTP/REST
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot (Java)                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐ │
│  │   RAG 控制器     │  │   知识图谱服务   │  │   业务上下文管理器          │ │
│  │ AiRagController  │  │ KnowledgeGraph  │  │ BusinessContextProvider    │ │
│  └────────┬────────┘  └────────┬────────┘  └──────────────┬──────────────┘ │
└───────────┼───────────────────┼──────────────────────────┼────────────────┘
            │                   │                          │
            │   HTTP/REST      │   Bolt                   │
            ▼                   ▼                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      FastAPI RAG Service (Python)                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                          RAG 核心服务                                    ││
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ ││
│  │  │  意图路由    │  │  知识检索    │  │  Prompt构建  │  │  LLM生成     │ ││
│  │  │ IntentRouter │  │ Qdrant搜索  │  │PromptBuilder│  │ ModelService │ ││
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘ ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐  │
│  │   Neo4j 图谱    │  │  Qdrant 向量库  │  │  Sentence-Transformers     │  │
│  │   知识图谱      │  │  作物病害知识   │  │  本地 Embedding 模型       │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 技术栈

| 层级 | 技术选型 | 说明 |
|-----|---------|-----|
| 前端 | Vue 3 + Element Plus | 响应式 Web 界面 |
| 后端 | Spring Boot 3.x | Java 企业级应用框架 |
| RAG 服务 | FastAPI + Pydantic | Python 高性能 API |
| 向量数据库 | Qdrant | 高性能向量相似度检索 |
| 知识图谱 | Neo4j | 图数据库存储实体关系 |
| Embedding | paraphrase-multilingual-MiniLM-L12-v2 | 本地多语言向量化模型 |
| LLM | 通义千问 / DeepSeek | 大语言模型（百炼 API） |

---

## 三、核心模块详解

### 3.1 意图路由模块 (Intent Router)

**文件位置**: `rag-service/app/services/intent_router.py`

**功能**: 基于规则引擎自动判断用户问题所属的知识领域，实现精准路由。

**支持的领域**:

| 领域标识 | 领域名称 | 关键词示例 | Qdrant 集合 |
|---------|---------|-----------|-------------|
| `crop_disease` | 作物病害与种植技术 | 倒伏、减产、病虫害、施肥、浇水 | agriculture_knowledge |
| `market_price` | 农产品价格与市场 | 价格、行情、成本、收益、批发 | agriculture_market_price |
| `agri_policy` | 农业政策与补贴 | 补贴、政策、贷款、保险 | agriculture_agri_policy |
| `general` | 通用农业 | 其他农业问题 | agriculture_knowledge |

**路由规则**:
- **技术优先**: 包含"倒伏、减产、病虫害"等词的问题，即使含有价格词，也优先路由到作物病害领域
- **纯价格识别**: 只有价格词没有技术词，才路由到市场领域
- **置信度计算**: 根据关键词匹配权重计算置信度，低于阈值降级为通用问题

```python
# 核心路由逻辑示例
if has_tech_keyword:
    return RouteResult(domain=KnowledgeDomain.CROP_DISEASE, ...)
elif has_price_keyword and not has_tech_keyword:
    return RouteResult(domain=KnowledgeDomain.MARKET_PRICE, ...)
```

### 3.2 向量检索模块 (Qdrant Service)

**文件位置**: `rag-service/app/services/qdrant_service.py`

**功能**: 管理 Qdrant 向量数据库，执行高效的向量相似度检索。

**核心方法**:

| 方法名 | 功能描述 |
|-------|---------|
| `search_collection()` | 在指定集合中检索相似文档 |
| `keyword_search()` | 关键词全文检索（兜底方案） |
| `multi_collection_search()` | 多集合联合检索 |

**检索流程**:
```
1. 接收查询向量 (384维)
2. 在目标集合中执行 ANN 检索
3. 过滤低于阈值的低相似度结果
4. 可选：Reranker 重排序
5. 返回 Top-K 结果（含分数）
```

**分数说明**:
- 范围: 0.0 ~ 1.0（余弦相似度）
- > 0.7: 高度相关
- 0.5 ~ 0.7: 中度相关
- 0.3 ~ 0.5: 低度相关
- < 0.3: 可能不相关

### 3.3 业务上下文模块

**文件位置**: `rag-service/app/services/business_context_provider.py`

**功能**: 从 Spring Boot 获取当前业务数据（预警、设备、农田等），作为上下文注入 LLM。

**数据来源**:
- 当前预警详情
- 关联设备状态
- 农田环境数据
- 作物生长信息
- 历史处置记录

### 3.4 Prompt 构建模块

**文件位置**: `rag-service/app/services/prompt_builder.py`

**功能**: 构建结构化的 Prompt，包含业务上下文和检索到的知识片段。

**Prompt 结构**:
```
[系统指令]
你是一个专业的智慧农业助手...

[业务上下文]
当前预警: 设备离线预警
关联农田: 昆山A+温室
关联设备: AGV运输车
...

[知识参考]
# 参考1: 作物倒伏补救措施
倒伏是作物生长过程中常见的...

# 参考2: 温室设备维护手册
AGV运输车需要定期检查...

[用户问题]
水稻倒伏了怎么补救？

[输出要求]
请以JSON格式回答...
```

### 3.5 LLM 生成模块

**文件位置**: `rag-service/app/services/model_service.py`

**功能**: 调用大语言模型生成结构化回答。

**输出格式**:
```json
{
  "answer_summary": "水稻倒伏后应立即排水...",
  "conclusion": "倒伏程度中等，建议人工扶正...",
  "possible_causes": ["暴雨导致", "根系浅", "种植密度过大"],
  "recommendations": ["排水扶正", "喷施叶面肥", "加强病虫害防治"],
  "priority": "高",
  "should_manual_review": true
}
```

---

## 四、RAG 检索流程

### 4.1 完整流程图

```
┌──────────┐    ┌──────────────┐    ┌────────────────┐    ┌────────────────┐
│  用户问题 │───▶│  意图路由    │───▶│  查询规划      │───▶│  向量检索      │
└──────────┘    └──────────────┘    └────────────────┘    └───────┬────────┘
                                                                    │
                    ┌────────────────┐    ┌────────────────┐        │
                    │  质量验证      │◀───│  相似度过滤    │◀───────┘
                    └───────┬────────┘    └────────────────┘
                            │
                            ▼
┌──────────┐    ┌──────────────┐    ┌────────────────┐    ┌────────────────┐
│  结构化回答│◀───│  LLM 生成    │◀───│  Prompt 构建   │◀───│  业务上下文    │
└──────────┘    └──────────────┘    └────────────────┘    └────────────────┘
```

### 4.2 步骤详解

#### 步骤 1: 意图路由
```python
route_result = intent_router.route(query)
# 返回: domain, confidence, matched_keywords
```

#### 步骤 2: 查询规划
```python
plan = query_planner.plan(request)
# 返回: target_collections, knowledge_top_k, min_similarity_score
```

#### 步骤 3: 知识检索
```python
knowledge_refs = rag_service._search_knowledge_with_filter(query, plan)
# 返回: List[RagReferenceItem], 每个包含 id, title, content, score
```

#### 步骤 4: 质量验证
```python
has_valid = rag_service._validate_retrieval_quality(refs, query)
# 检查噪音率、关键词匹配度
```

#### 步骤 5: Prompt 构建
```python
prompt = prompt_builder.build(user_query, plan, business_context, knowledge_refs)
```

#### 步骤 6: LLM 生成
```python
structured = model_service.generate_structured_answer(prompt, plan)
```

#### 步骤 7: 响应封装
```python
response = RagAnswerResponse(
    answer_summary=structured.answer_summary,
    cited_knowledge_sources=[...],
    cited_knowledge_graph=[...],
    retrieval_metadata={"max_score": 0.87}
)
```

---

## 五、前端交互

### 5.1 交互模式

| 模式 | 标识 | 行为描述 |
|-----|------|---------|
| 纯知识模式 | `knowledge` | 仅检索知识库，不关联业务数据 |
| 业务联动模式 | `business` | 关联当前预警/设备，提供针对性建议 |
| 智能混合模式 | `hybrid` | 同时检索知识库和业务上下文（默认） |
| 自动模式 | `auto` | 系统自动选择最佳模式 |

### 5.2 显示内容

**检索与可信度面板**:
- **向量相似度**: 0.0~1.0，显示与知识库片段的向量接近程度
- **模式标签**: 当前使用的 RAG 模式
- **来源卡片**: 显示检索到的知识来源列表

**向量相似度说明**:
- 仅表示「问题与知识库片段」的向量接近程度
- 数值偏低时检索可能不够贴切
- 不代表农艺结论已验证为真

---

## 六、API 接口

### 6.1 RAG 回答接口

**端点**: `POST /rag/answer`

**请求体**:
```json
{
  "query": "水稻倒伏了怎么补救？",
  "mode": "hybrid",
  "related_warning_id": 123,
  "related_device_id": 8,
  "related_farmland_id": 4,
  "cited_knowledge_graph": [...]
}
```

**响应体**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "session_id": "rag-abc123",
    "mode": "hybrid",
    "question_type": "hybrid",
    "answer_summary": "水稻倒伏后应立即采取以下措施...",
    "conclusion": "建议优先排水，然后人工扶正...",
    "possible_causes": ["暴雨冲击", "根系发育不良"],
    "recommendations": ["及时排水", "人工扶正", "喷施叶面肥"],
    "priority": "高",
    "should_manual_review": true,
    "cited_knowledge_sources": [
      {"id": "k001", "title": "水稻倒伏预防与补救", "score": 0.87, "source": "知识库"}
    ],
    "cited_knowledge_graph": [
      {"id": "水稻", "title": "水稻", "kind": "CROP", "subgraph": {...}}
    ],
    "retrieval_metadata": {
      "max_score": 0.87,
      "intent_domain": "crop_disease",
      "retrieved_count": 3
    },
    "fallback": false,
    "fallback_reason": null
  }
}
```

### 6.2 知识检索接口

**端点**: `POST /rag/search`

**请求体**:
```json
{
  "query": "番茄叶片发黄",
  "top_k": 5,
  "intent_domain": "crop_disease"
}
```

### 6.3 用户反馈接口

**端点**: `POST /rag/feedback`

**请求体**:
```json
{
  "session_id": "rag-abc123",
  "question": "水稻倒伏了怎么补救？",
  "answer_id": "optional-id",
  "is_helpful": true,
  "feedback_text": "回答很专业",
  "retrieved_refs": ["k001", "k002"]
}
```

---

## 七、知识库管理

### 7.1 知识库结构

```
rag-service/
├── knowledge/                    # 本地知识库
│   ├── base/                    # 基础农业知识
│   ├── crop/                    # 作物种植知识
│   │   └── rice-tillering-water-management.md
│   ├── device/                  # 设备操作手册
│   └── sop/                     # 标准作业流程
├── data/                        # 抓取的知识数据
│   ├── raw/                     # 原始数据
│   └── cleaned/                 # 清洗后数据
└── scripts/
    └── index_knowledge.py       # 知识导入脚本
```

### 7.2 知识导入流程

1. **数据准备**: 将知识文档放入 `knowledge/` 目录
2. **文本清洗**: 移除格式标记、特殊字符
3. **分块处理**: 按 chunk_size=500、chunk_overlap=80 分块
4. **向量化**: 使用本地 Embedding 模型生成 384 维向量
5. **入库**: 批量写入 Qdrant 集合

### 7.3 Qdrant 集合配置

| 集合名称 | 用途 | 向量维度 | 距离度量 |
|---------|-----|---------|---------|
| agriculture_knowledge | 通用农业知识 | 384 | Cosine |
| agriculture_crop_disease | 作物病害知识 | 384 | Cosine |
| agriculture_market_price | 价格行情知识 | 384 | Cosine |
| agriculture_agri_policy | 政策补贴知识 | 384 | Cosine |

---

## 八、配置说明

### 8.1 环境变量 (.env)

```bash
# ===== 应用配置 =====
APP_PORT=9001
APP_ENV=dev

# ===== Qdrant 配置 =====
QDRANT_HOST=localhost
QDRANT_PORT=6333
QDRANT_COLLECTION_AGRICULTURE=agriculture_knowledge
QDRANT_VECTOR_SIZE=384
QDRANT_TIMEOUT_SECONDS=10

# ===== 向量模型（本地 sentence-transformers）=====
EMBEDDING_PROVIDER=local
LOCAL_EMBEDDING_MODEL=paraphrase-multilingual-MiniLM-L12-v2
EMBEDDING_DIMENSIONS=384

# ===== LLM 配置 =====
LLM_PROVIDER=openai
LLM_MODEL=deepseek-chat
OPENAI_BASE_URL=https://api.deepseek.com/v1
OPENAI_API_KEY=sk-xxxx
LLM_TEMPERATURE=0.35
LLM_MAX_TOKENS=3072

# ===== Neo4j 图谱配置 =====
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=<与根目录 .env 中 SMARTAGRI_NEO4J_PASSWORD 保持一致，勿写死真实口令>

# ===== Spring Boot 对接 =====
SPRING_BACKEND_BASE_URL=http://localhost:8080/api
```

### 8.2 检索参数配置

```bash
# 相似度阈值
RETRIEVAL_MIN_SIMILARITY_SCORE=0.5

# 检索数量
RETRIEVAL_DEFAULT_TOP_K=10

# Reranker 配置（可选）
RERANKER_ENABLED=false
RERANKER_API_URL=http://localhost:8000/rerank
```

---

## 九、部署指南

### 9.1 Docker Compose 部署

```bash
# 一键启动所有服务
docker compose up -d

# 服务端口
# - MySQL: 3307
# - Neo4j: 7474/7687
# - Qdrant: 6333/6334
# - RAG Service: 9001
# - Spring Boot: 8090
# - Web: 8088
```

### 9.2 本地开发部署

**1. 启动依赖服务**:
```bash
# 启动 Neo4j
docker compose -f docker-compose.neo4j.yml up -d

# 启动 Qdrant
docker compose -f docker-compose.yml up -d qdrant
```

**2. 启动 RAG Service**:
```bash
cd rag-service
pip install -r requirements.txt
python -m app.main
```

**3. 启动 Spring Boot**:
```bash
cd smart-agriculture-server
mvn spring-boot:run
```

**4. 启动前端**:
```bash
cd smart-agriculture-web
npm install
npm run dev
```

### 9.3 健康检查

```bash
# RAG Service 健康检查
curl http://localhost:9001/health

# Qdrant 健康检查
curl http://localhost:6333/health
```

---

## 十、运维指南

### 10.1 日志查看

**RAG Service 日志**:
```bash
# 实时查看日志
tail -f rag-service/logs/rag-service.log

# 查看错误日志
grep ERROR rag-service/logs/rag-service.log
```

**Spring Boot 日志**:
```bash
# 实时查看 RAG 相关日志
tail -f smart-agriculture-server/logs/spring.log | grep -i rag
```

### 10.2 常见问题排查

| 问题现象 | 可能原因 | 解决方案 |
|---------|---------|---------|
| RAG 回答为空 | Qdrant 检索失败 | 检查 Qdrant 服务状态 |
| 向量相似度显示 "--" | RAG 服务不可用 | 检查 FastAPI 是否启动 |
| 兜底模式激活 | LLM 调用失败 | 检查 API Key 和网络 |
| 检索结果为空 | 知识库未导入 | 运行知识导入脚本 |

### 10.3 知识库维护

**添加新知识**:
1. 将文档放入 `rag-service/knowledge/` 目录
2. 运行导入脚本: `python scripts/index_knowledge.py`
3. 验证导入: `curl http://localhost:6333/collections/agriculture_knowledge`

**重建向量索引**:
```bash
# 删除现有集合
curl -X DELETE http://localhost:6333/collections/agriculture_knowledge

# 重新导入知识
python scripts/index_knowledge.py
```

---

## 十一、扩展指南

### 11.1 添加新知识领域

1. **定义领域枚举** (`intent_router.py`):
```python
class KnowledgeDomain(str, Enum):
    NEW_DOMAIN = "new_domain"
```

2. **添加关键词规则**:
```python
DOMAIN_PATTERNS[KnowledgeDomain.NEW_DOMAIN] = [
    (r"新关键词", 0.9),
    ...
]
```

3. **创建 Qdrant 集合**:
```bash
curl -X PUT http://localhost:6333/collections/new_domain
```

### 11.2 集成新 LLM

1. **实现 Provider** (`app/services/llm/`):
```python
class NewLLMProvider:
    def __init__(self, api_key: str, model: str):
        ...
    
    def generate(self, prompt: str) -> str:
        ...
```

2. **配置使用** (`.env`):
```bash
LLM_PROVIDER=new
NEW_LLM_API_KEY=xxx
NEW_LLM_MODEL=xxx
```

---

## 附录 A: 项目结构

```
zhihuinongye/
├── rag-service/                          # RAG 服务 (Python)
│   ├── app/
│   │   ├── api/                         # API 路由
│   │   │   └── routes/rag.py            # RAG 接口
│   │   ├── core/                        # 核心配置
│   │   │   ├── config.py                # 环境配置
│   │   │   ├── qdrant_client.py         # Qdrant 客户端
│   │   │   └── response.py               # 统一响应格式
│   │   ├── schemas/                      # Pydantic 模型
│   │   │   └── rag.py                   # RAG 请求/响应模型
│   │   ├── services/                    # 业务服务
│   │   │   ├── rag_service.py           # RAG 核心
│   │   │   ├── intent_router.py          # 意图路由
│   │   │   ├── qdrant_service.py        # 向量检索
│   │   │   ├── model_service.py         # LLM 调用
│   │   │   ├── prompt_builder.py         # Prompt 构建
│   │   │   └── embedding/               # 向量化服务
│   │   └── main.py                       # FastAPI 入口
│   ├── knowledge/                        # 本地知识库
│   ├── data/                            # 抓取数据
│   └── scripts/                         # 工具脚本
├── smart-agriculture-server/              # Spring Boot 后端 (Java)
│   └── src/main/java/com/smartagri/
│       └── modules/ai/rag/              # RAG 模块
│           ├── controller/               # REST 控制器
│           ├── service/impl/            # RAG 服务实现
│           ├── client/                   # FastAPI 客户端
│           └── vo/                       # 视图对象
├── smart-agriculture-web/                 # Vue 前端
│   └── src/views/ai/
│       └── AiAssistant.vue              # AI 助手页面
├── docker/                              # Docker 配置
├── docker-compose.yml                    # 完整栈编排
└── docker-compose.neo4j.yml             # Neo4j 独立部署
```

## 附录 B: 数据流向图

```
用户提问
    │
    ▼
Spring Boot (AiRagController)
    │
    ├─▶ 获取业务上下文 (预警/设备/农田)
    │
    ├─▶ 获取知识图谱上下文 (Neo4j)
    │
    ▼
FastAPI RAG Service (/rag/answer)
    │
    ├─▶ 意图路由 → 确定知识领域
    │
    ├─▶ 向量检索 (Qdrant)
    │       │
    │       ├─▶ Embedding 查询向量
    │       ├─▶ ANN 相似度搜索
    │       └─▶ 相似度过滤
    │
    ├─▶ Prompt 构建
    │       │
    │       ├─▶ 业务上下文
    │       ├─▶ 知识检索结果
    │       └─▶ 图谱上下文
    │
    ├─▶ LLM 生成 (通义千问/DeepSeek)
    │
    ▼
返回结构化回答
    │
    ├─▶ answer_summary (正文摘要)
    ├─▶ conclusion (要点小结)
    ├─▶ possible_causes (可能原因)
    ├─▶ recommendations (建议措施)
    ├─▶ cited_knowledge_sources (知识来源)
    ├─▶ cited_knowledge_graph (图谱引用)
    └─▶ retrieval_metadata (检索元数据)
```

---

**文档版本**: 1.0  
**最后更新**: 2026-04-13  
**维护者**: 智慧农业开发团队
