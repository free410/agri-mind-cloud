import json
import logging
import re

import httpx

from app.core.config import settings
from app.schemas.rag import BuiltBusinessContext, ModelStructuredOutput, QueryPlan, RagReferenceItem

logger = logging.getLogger(__name__)


class RagModelService:
    """
    模型服务封装：
    - 当前默认 mock，可稳定演示
    - 预留真实模型接入点
    """

    def generate_structured_answer(
        self,
        prompt: str,
        plan: QueryPlan,
        business_context: BuiltBusinessContext | None,
        knowledge_refs: list[RagReferenceItem],
    ) -> ModelStructuredOutput:
        provider = settings.llm_provider.lower().strip()
        if provider in ("mock", "none"):
            return self._mock_output(plan, business_context, knowledge_refs)

        if provider in ("openai", "openai-compatible", "bailian", "dashscope"):
            if not (settings.openai_api_key or "").strip():
                logger.warning(
                    "LLM_PROVIDER=%s 但未配置 OPENAI_API_KEY，已返回本地 mock 结构化结果（演示用）。",
                    settings.llm_provider,
                )
                return self._mock_output(plan, business_context, knowledge_refs)
            try:
                return self._call_openai_compatible_chat(prompt)
            except Exception:
                if settings.llm_fallback_to_mock:
                    return self._mock_output(plan, business_context, knowledge_refs)
                raise

        raise ValueError(f"不支持的 LLM_PROVIDER: {settings.llm_provider}")

    def _call_openai_compatible_chat(self, prompt: str) -> ModelStructuredOutput:
        url = f"{settings.openai_base_url.rstrip('/')}/chat/completions"
        headers = {
            "Authorization": f"Bearer {settings.openai_api_key.strip()}",
            "Content-Type": "application/json",
        }
        json_hint = (
            "请只输出一个 JSON 对象，不要 Markdown 代码块。各字段要求如下：\n"
            "· answer_summary：字符串，建议不少于 200 个汉字；用 2～4 个短自然段，综合业务与知识片段，说明背景、关键事实与要点。\n"
            "· conclusion：字符串，建议不少于 80 个汉字；给出明确判断或优先动作，可适当使用农艺/设备专业用语。\n"
            "· possible_causes：字符串数组，4～8 条，从环境、栽培、设备、管理等多角度具体展开，避免空泛。\n"
            "· recommendations：字符串数组，5～10 条，按可执行顺序排列，可含注意事项、复核时机。\n"
            "· priority：仅允许「低」「中」「高」；should_manual_review：布尔。\n"
            "务必化用或引用上文「知识检索片段」「业务上下文」「知识图谱关联」中的信息；无依据处勿编造数据。"
        )
        body = {
            "model": settings.llm_model,
            "temperature": settings.llm_temperature,
            "max_tokens": settings.llm_max_tokens,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "你是资深智慧农业技术顾问，面向农技员与管理员，回答须信息量大、可落地。"
                        "在仅输出合法 JSON 的前提下，尽量写长、写细，避免口号式短句。"
                    ),
                },
                {"role": "user", "content": f"{prompt}\n\n{json_hint}"},
            ],
        }
        with httpx.Client(timeout=settings.openai_timeout_seconds) as client:
            resp = client.post(url, json=body, headers=headers)
            resp.raise_for_status()
            data = resp.json()

        err = data.get("error")
        if err:
            raise RuntimeError(err.get("message") or str(err))

        choices = data.get("choices") or []
        if not choices:
            raise RuntimeError("模型未返回 choices")
        content = (choices[0].get("message") or {}).get("content") or ""
        parsed = self._extract_json_object(content)
        return ModelStructuredOutput.model_validate(parsed)

    @staticmethod
    def _extract_json_object(text: str) -> dict:
        text = text.strip()
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            pass
        m = re.search(r"\{[\s\S]*\}", text)
        if not m:
            raise ValueError("响应中未找到 JSON 对象")
        return json.loads(m.group(0))

    @staticmethod
    def _mock_output(
        plan: QueryPlan,
        business_context: BuiltBusinessContext | None,
        knowledge_refs: list[RagReferenceItem],
    ) -> ModelStructuredOutput:
        risk_signals = business_context.risk_signals if business_context else []
        has_business = bool(business_context and business_context.has_business_data)

        kpreview = ""
        if knowledge_refs:
            kpreview = " ".join(f"《{r.title}》摘录要点：{r.content[:120]}…" for r in knowledge_refs[:3])
        answer_summary = (
            f"本次为「{plan.question_type}」场景下的综合分析。"
            f"业务侧{'已接入实时上下文' if has_business else '未接入或已关闭业务通道'}，"
            f"知识库检索命中 {len(knowledge_refs)} 条相关材料。"
            f"以下先给出可读性较强的执行摘要，便于管理人员快速把握现场与农艺要点。"
            f"{' ' + kpreview if kpreview else ''}"
        )
        conclusion = (
            "综合业务信号与知识材料，当前宜以「确认现场—稳住环境—再谈农艺调整」为优先顺序；"
            "若存在多源告警叠加，应提高复核等级并保留完整处置痕迹。"
            if has_business
            else "在缺少实时业务曲线与设备状态时，结论以知识侧通识与规程为主；"
            "建议补采环境、设备与农事记录后再做针对性处方。"
        )

        possible_causes = (
            list(risk_signals[:6])
            if risk_signals
            else [
                "监测指标短时波动或阈值配置偏紧导致误报风险",
                "采集链路（传感器/网关/供电）异常造成数据缺口",
                "田间小气候与预报不一致，作物响应滞后",
                "设备维护窗口未同步到调度系统",
                "历史同期类似事件未形成闭环知识沉淀",
            ]
        )
        recommendations = [
            "拉取最近 24～48h 环境与设备原始曲线，核对触发时刻是否一致",
            "按预警类型打开对应 SOP，逐步勾选「已核实项」避免跳步",
            "现场拍照或记录编号，关联到预警单便于审计",
            "对关键设备做连通性与固件版本复核",
            "处置后回填结果并设置复盘时间（如 72h 内）",
            "若涉及农艺措施，同步通知种植负责人确认肥水与用药窗口期",
        ]
        if plan.question_type == "knowledge":
            recommendations = [
                "对照知识片段中的生育期与逆境类型，先核对本地是否处于相同阶段",
                "将环境阈值（温光水气）与片段建议区间逐项比对",
                "小面积试处置并设对照，记录 3～5 天表型变化",
                "涉及投入品时核对登记范围与安全间隔期",
                "整理为可复用的田块档案条目，便于下次相似问题快速调用",
                "必要时升级专家会诊或第三方检测，避免单凭经验扩大面积用药",
            ]

        if plan.question_type == "knowledge":
            priority = "中"
        elif risk_signals:
            priority = "高"
        else:
            priority = "中"

        should_manual_review = plan.question_type != "knowledge" or (has_business and len(risk_signals) > 0)

        return ModelStructuredOutput(
            answer_summary=answer_summary,
            conclusion=conclusion,
            possible_causes=possible_causes,
            recommendations=recommendations,
            priority=priority,  # type: ignore[arg-type]
            should_manual_review=should_manual_review,
        )


model_service = RagModelService()
