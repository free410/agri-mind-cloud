import hashlib
import json
from pathlib import Path

from app.models.knowledge_models import RawKnowledgeDocument


class KnowledgeDocumentLoader:
    SUPPORTED_EXTS = {".md", ".txt", ".json"}

    SOURCE_TYPE_MAPPING = {
        "base": "manual",
        "sop": "sop",
        "crop": "crop_guide",
        "device": "device_doc",
        "rule": "rule",
    }

    def load_documents(self, root_dir: Path) -> list[RawKnowledgeDocument]:
        if not root_dir.exists():
            return []

        documents: list[RawKnowledgeDocument] = []
        skip_names = {"readme.md", "readme.txt"}
        for file_path in sorted(root_dir.rglob("*")):
            if not file_path.is_file():
                continue
            if file_path.name.lower() in skip_names:
                continue
            if file_path.suffix.lower() not in self.SUPPORTED_EXTS:
                continue
            documents.extend(self._load_file(root_dir, file_path))
        return documents

    def load_cleaned_jsonl_dir(self, cleaned_dir: Path) -> list[RawKnowledgeDocument]:
        """加载抓取清洗结果（每行一条 CleanedKnowledgeRecord JSON）。"""
        if not cleaned_dir.is_dir():
            return []
        documents: list[RawKnowledgeDocument] = []
        for jsonl_path in sorted(cleaned_dir.glob("*.jsonl")):
            documents.extend(self._load_cleaned_jsonl_file(jsonl_path))
        return documents

    def _load_cleaned_jsonl_file(self, file_path: Path) -> list[RawKnowledgeDocument]:
        relative = file_path.as_posix()
        docs: list[RawKnowledgeDocument] = []
        try:
            raw_lines = file_path.read_text(encoding="utf-8").splitlines()
        except OSError:
            return []

        for line_no, line in enumerate(raw_lines, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                row = json.loads(line)
            except json.JSONDecodeError:
                continue
            if not isinstance(row, dict):
                continue
            content = str(row.get("content") or "").strip()
            if not content:
                continue
            doc_id = str(row.get("doc_id") or "").strip() or self._hash_id(f"{relative}#{line_no}")
            title = str(row.get("title") or f"article-{line_no}").strip()
            source_name = str(row.get("source_name") or file_path.stem).strip()
            source_type = str(row.get("source_type") or "manual").strip()
            topic = row.get("topic")
            topic_s = str(topic).strip() if topic is not None else None
            crop = row.get("crop_type")
            crop_s = str(crop).strip() if crop is not None else None
            warn = row.get("warning_type")
            warn_s = str(warn).strip() if warn is not None else None
            source_url = str(row.get("source_url") or "").strip()
            tail = doc_id if doc_id else f"L{line_no}"
            source_path = f"{relative}#{tail}"
            text = content
            if source_url:
                text = f"来源：{source_url}\n\n{content}"

            docs.append(
                self._build_document(
                    doc_id=doc_id,
                    source_path=source_path,
                    source_name=source_name,
                    source_type=source_type,
                    title=title,
                    text=text,
                    ext=".jsonl",
                    topic=topic_s,
                    crop_type=crop_s,
                    device_type=None,
                    warning_type=warn_s if warn_s else None,
                )
            )
        return docs

    def _load_file(self, root_dir: Path, file_path: Path) -> list[RawKnowledgeDocument]:
        ext = file_path.suffix.lower()
        relative_path = file_path.relative_to(root_dir).as_posix()
        source_bucket = relative_path.split("/", 1)[0] if "/" in relative_path else relative_path.split(".")[0]
        source_type = self.SOURCE_TYPE_MAPPING.get(source_bucket, "manual")
        source_name = file_path.stem

        if ext in {".md", ".txt"}:
            text = self._read_text_file(file_path)
            if not text.strip():
                return []
            return [
                self._build_document(
                    doc_id=self._hash_id(f"{relative_path}#0"),
                    source_path=relative_path,
                    source_name=source_name,
                    source_type=source_type,
                    title=source_name,
                    text=text,
                    ext=ext,
                )
            ]

        if ext == ".json":
            return self._load_json_documents(
                file_path=file_path,
                relative_path=relative_path,
                source_name=source_name,
                source_type=source_type,
                ext=ext,
            )

        return []

    def _load_json_documents(
        self,
        file_path: Path,
        relative_path: str,
        source_name: str,
        source_type: str,
        ext: str,
    ) -> list[RawKnowledgeDocument]:
        try:
            raw = self._read_text_file(file_path)
            data = json.loads(raw)
        except Exception:
            return []

        records: list[dict] = []
        if isinstance(data, dict):
            if isinstance(data.get("documents"), list):
                records = [item for item in data["documents"] if isinstance(item, dict)]
            elif any(key in data for key in ("content", "text")):
                records = [data]
        elif isinstance(data, list):
            records = [item for item in data if isinstance(item, dict)]

        docs: list[RawKnowledgeDocument] = []
        for idx, item in enumerate(records):
            text = str(item.get("content") or item.get("text") or "").strip()
            if not text:
                continue

            title = str(item.get("title") or f"{source_name}-{idx + 1}")
            docs.append(
                self._build_document(
                    doc_id=self._hash_id(f"{relative_path}#{idx}"),
                    source_path=relative_path,
                    source_name=source_name,
                    source_type=str(item.get("source_type") or source_type),
                    title=title,
                    text=text,
                    ext=ext,
                    topic=item.get("topic"),
                    crop_type=item.get("crop_type"),
                    device_type=item.get("device_type"),
                    warning_type=item.get("warning_type"),
                )
            )
        return docs

    def _build_document(
        self,
        doc_id: str,
        source_path: str,
        source_name: str,
        source_type: str,
        title: str,
        text: str,
        ext: str,
        topic: str | None = None,
        crop_type: str | None = None,
        device_type: str | None = None,
        warning_type: str | None = None,
    ) -> RawKnowledgeDocument:
        inferred_topic = topic or self._infer_topic(f"{title}\n{text}")
        inferred_warning_type = warning_type or self._infer_warning_type(f"{title}\n{text}")
        return RawKnowledgeDocument(
            doc_id=doc_id,
            source_path=source_path,
            source_name=source_name,
            source_type=source_type,
            title=title,
            text=text,
            ext=ext,
            topic=inferred_topic,
            crop_type=crop_type,
            device_type=device_type,
            warning_type=inferred_warning_type,
        )

    @staticmethod
    def _read_text_file(file_path: Path) -> str:
        for encoding in ("utf-8", "utf-8-sig", "gbk"):
            try:
                return file_path.read_text(encoding=encoding)
            except UnicodeDecodeError:
                continue
        return file_path.read_text(errors="ignore")

    @staticmethod
    def _hash_id(seed: str) -> str:
        return hashlib.md5(seed.encode("utf-8")).hexdigest()

    @staticmethod
    def _infer_topic(text: str) -> str:
        normalized = text.lower()
        if "湿度" in text or "humidity" in normalized:
            return "humidity"
        if "灌溉" in text or "irrigation" in normalized:
            return "irrigation"
        if "设备" in text or "device" in normalized:
            return "device"
        if "作物" in text or "crop" in normalized:
            return "crop"
        return "warning"

    @staticmethod
    def _infer_warning_type(text: str) -> str | None:
        if "土壤湿度" in text:
            return "土壤湿度过低"
        if "温度过高" in text:
            return "温度过高"
        if "温度过低" in text:
            return "温度过低"
        if "设备离线" in text:
            return "设备离线"
        if "光照" in text:
            return "光照异常"
        return None
