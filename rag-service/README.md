# SmartAgri RAG Service

本目录为智慧农业管理系统新增的 FastAPI + Qdrant RAG 服务层，负责：

- 知识库切片与向量入库
- 业务上下文 + 知识库双通道 RAG 问答
- 公开农业知识抓取与清洗

## 1. 本地启动

```bash
cd rag-service
python -m venv .venv
```

Windows:

```bash
.venv\Scripts\activate
```

macOS / Linux:

```bash
source .venv/bin/activate
```

安装依赖：

```bash
pip install -r requirements.txt
```

复制环境变量模板：

```bash
copy .env.example .env
```

启动服务：

```bash
uvicorn app.main:app --host 0.0.0.0 --port 9001 --reload
```

默认地址：

- 服务地址：`http://localhost:9001`
- OpenAPI：`http://localhost:9001/docs`

## 2. 当前接口

- `GET /health`
- `POST /rag/search`
- `POST /rag/answer`
- `POST /rag/briefing`
- `GET /knowledge/crawl/status`
- `POST /knowledge/crawl`

## 3. 公开农业知识抓取

### 3.1 首批来源

- 中国农业农村信息网：`https://www.agri.cn`
- 中国政府网农业政策公开页：`https://www.gov.cn`
- 农业农村部公开页：已保留来源配置，但因 `robots.txt` 当前为 `Disallow: /`，默认禁用，不会抓取

抓取源配置文件：

```text
sources/china_agri_sources.yaml
```

### 3.2 数据落盘

抓取结果会保存为两份：

- 原始结果：`data/raw/*.jsonl`
- 清洗结果：`data/cleaned/*.jsonl`

增量状态保存在：

- `data/cleaned/crawl_manifest.json`

### 3.3 执行抓取

```bash
python app/scripts/crawl_china_agri_knowledge.py
```

常用参数：

```bash
python app/scripts/crawl_china_agri_knowledge.py --limit 5
python app/scripts/crawl_china_agri_knowledge.py --source 中国农业农村信息网 --limit 3
python app/scripts/crawl_china_agri_knowledge.py --force --limit 2
```

### 3.4 抓取正文清洗与农技筛选

- **入库前清洗**（已实现）：解析 HTML 后会对正文执行 `clean_for_index()`，去除责任编辑、来源行、分享引导等噪声，减轻向量污染。
- **农技向筛选**（可选）：综合新闻与生产技术混杂时，可先筛再灌库：

```bash
python scripts/filter_cleaned_jsonl.py --input data/cleaned --output data/cleaned_agronomy --min-score 2
```

说明：`--min-score` 为标题+正文中「农技关键词」命中种类数下限；默认还会丢弃标题含党建/会议/禁渔等非生产向文章。筛结果写入 `data/cleaned_agronomy/*.jsonl`。

## 4. 知识库导入

### 4.1 本地知识文档目录

- `knowledge/base/`
- `knowledge/sop/`
- `knowledge/crop/`（作物栽培、灌溉、生育期等，**推荐主力放农技 md**）
- `knowledge/tech/`（与 crop 相同类型 `crop_guide`，便于单独管理规程/转化 PDF 的 md）
- `knowledge/device/`

支持：

- `.md`
- `.txt`
- `.json`

### 4.2 导入 Qdrant

```bash
python scripts/index_knowledge.py
```

常用参数：

```bash
python scripts/index_knowledge.py --dry-run
python scripts/index_knowledge.py --recreate
python scripts/index_knowledge.py --chunk-size 520 --overlap 100
```

仅使用 **农技筛过后的抓取结果**（不包含默认 `data/cleaned` 全量）时：

```bash
python scripts/index_knowledge.py --cleaned-dir data/cleaned_agronomy
```

只要本地 `knowledge/`、**不要**任何抓取 jsonl：

```bash
python scripts/index_knowledge.py --skip-cleaned
```

**推荐实践**：以 `knowledge/crop/`、`knowledge/tech/` 自建农技语料为主；抓取新闻仅作补充，并用 `filter_cleaned_jsonl.py` 控制入库质量。

## 5. 抓取文档 metadata

清洗后的知识文档至少包含：

- `doc_id`
- `title`
- `content`
- `source_name`
- `source_url`
- `domain`
- `publish_time`
- `crawl_time`
- `source_type`
- `topic`
- `crop_type`
- `warning_type`
- `topic_tags`
- `url_hash`
- `content_hash`

## 6. 说明

第一版抓取器强调：

- 公开来源
- robots 合规
- 可重复执行
- 可增量更新
- 适合比赛演示与后续接入 Qdrant
