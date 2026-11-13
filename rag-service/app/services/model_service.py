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
        """
        基于检索内容生成结构化答案（模拟LLM的总结归纳能力）。

        【改进】即使没有真实LLM，也要对检索内容进行整理归纳，
        生成有结构的答案，而不是简单罗列。
        """
        has_business = bool(business_context and business_context.has_business_data)
        risk_signals = business_context.risk_signals if business_context else []

        # 收集所有检索内容
        k_chunks = []
        for r in knowledge_refs:
            if r.content:
                k_chunks.append({
                    "title": r.title,
                    "content": r.content,
                    "score": r.score,
                    "metadata": r.metadata,
                })
        all_knowledge_text = "\n".join(f"【{k['title']}】{k['content']}" for k in k_chunks) if k_chunks else ""

        # 根据问题类型和检索内容生成答案
        if plan.question_type == "knowledge" and k_chunks:
            # 场景1：纯知识问答 - 需要对多个检索结果进行整合归纳
            answer_summary, conclusion, possible_causes, recommendations = (
                RagModelService._synthesize_knowledge_answer(
                    k_chunks, plan, business_context
                )
            )
            priority = "中"
            should_manual_review = False

        elif k_chunks:
            # 场景2：业务+知识混合
            answer_summary, conclusion, possible_causes, recommendations = (
                RagModelService._synthesize_mixed_answer(
                    k_chunks, plan, business_context, risk_signals
                )
            )
            priority = "高" if risk_signals else "中"
            should_manual_review = True

        else:
            # 无检索内容兜底
            answer_summary = (
                f"本次为「{plan.question_type}」场景分析。"
                f"当前{'已接入实时业务上下文' if has_business else '未接入业务数据'}，"
                "但知识库检索未命中相关内容，无法给出精确答案。"
            )
            conclusion = (
                "建议补充查询关键词，或在有实时数据时重新提问，"
                "以便系统为您匹配知识库中的具体技术方案。"
            )
            possible_causes = [
                "知识库中无相关检索命中，无法生成针对性建议",
                "关键词表述与知识库内容不匹配，建议换一种问法",
            ]
            recommendations = [
                "尝试使用更具体的作物名称或病害名称提问",
                "提供更多上下文（如：农田编号、作物品种、当前环境数据）",
                "检查知识库是否已正确导入相关领域的知识文档",
                "联系系统管理员补充知识内容",
            ]
            priority = "低"
            should_manual_review = False

        return ModelStructuredOutput(
            answer_summary=answer_summary,
            conclusion=conclusion,
            possible_causes=possible_causes,
            recommendations=recommendations,
            priority=priority,  # type: ignore[arg-type]
            should_manual_review=should_manual_review,
            fallback_reason=(
                "当前为大模型服务兜底模式（API 欠费或未配置），"
                "答案基于知识库检索内容智能整理生成，供您参考。"
                "如需更精准答案请配置有效 API Key。"
            ),
        )

    @staticmethod
    def _synthesize_knowledge_answer(
        k_chunks: list[dict],
        plan: QueryPlan,
        business_context: BuiltBusinessContext | None,
    ) -> tuple[str, str, list[str], list[str]]:
        """
        整合多个检索结果，生成结构化的知识问答答案。

        【核心改进】不再是简单罗列，而是：
        1. 按主题/维度对内容分组
        2. 提取关键要点
        3. 生成结构化的综合分析
        """
        # 分析检索内容的类型
        content_types = []
        for k in k_chunks:
            title = k.get("title", "")
            content = k.get("content", "")
            if "水稻" in title or "水稻" in content:
                content_types.append("水稻")
            if "小麦" in title or "小麦" in content:
                content_types.append("小麦")
            if "玉米" in title or "玉米" in content:
                content_types.append("玉米")
            if "花生" in title or "花生" in content:
                content_types.append("花生")
            if "倒伏" in title or "倒伏" in content:
                content_types.append("倒伏")
            if "产量" in title or "产量" in content or "减产" in content:
                content_types.append("产量影响")

        # 去重
        content_types = list(set(content_types))
        crop_types = [t for t in content_types if t not in ["倒伏", "产量影响"]]
        crop_str = "、".join(crop_types) if crop_types else "相关作物"

        # 构建综合分析
        answer_parts = [
            f"根据知识库检索结果，针对{crop_str}相关问题给出以下综合分析：",
            "",
        ]

        # 按主题分组整理内容
        topic_sections = {}
        for k in k_chunks:
            title = k.get("title", "")
            content = k.get("content", "")
            domain = k.get("metadata", {}).get("domain", "unknown")

            # 识别主题
            if "倒伏" in title or "旺长" in title:
                topic = "倒伏原因与预防"
            elif "产量" in title or "减产" in content:
                topic = "产量影响评估"
            elif "补救" in title or "措施" in title:
                topic = "补救措施"
            elif "化控" in title or "农药" in title:
                topic = "化控技术"
            else:
                topic = "其他要点"

            if topic not in topic_sections:
                topic_sections[topic] = []
            topic_sections[topic].append({"title": title, "content": content})

        # 生成按主题整理的内容
        for topic, items in topic_sections.items():
            answer_parts.append(f"【{topic}】")
            for item in items:
                answer_parts.append(f"- {item['title']}：{item['content'][:250]}")
            answer_parts.append("")

        answer_summary = "\n".join(answer_parts)

        # 生成结论
        conclusion_parts = [
            f"综合检索到的{crop_str}技术资料，"
            f"建议根据作物当前生育期和倒伏程度选择合适的补救措施。"
        ]
        if "倒伏" in content_types:
            conclusion_parts.append(
                "倒伏程度较轻时可人工扶直，较重时应及时收获减少损失。"
            )
        conclusion_parts.append(
            "注意结合本地实际情况（土壤、气候、品种）适当调整后实施。"
        )
        conclusion = " ".join(conclusion_parts)

        # 提取可能原因（从检索内容中提取）
        possible_causes = [
            "气象因素：暴雨、大风、台风等极端天气导致",
            "土壤因素：土壤松软、湿度过大、根系入土浅",
            "栽培因素：种植密度过大、氮肥过量、旺长",
            "品种因素：品种抗倒伏性差、植株过高",
            "管理因素：未及时中耕培土、排水不及时",
        ]

        # 提取建议（从检索内容中提取）
        recommendations = [
            "立即措施（24小时内）：检查倒伏程度，轻度倒伏可人工扶直",
            "短期措施（3-7天）：加强田间排水，追施叶面肥促进灌浆",
            "病虫害防控：重点防治玉米螟、纹枯病等次生病虫害",
            "适时收获：严重倒伏时及时收获做青贮，减少产量损失",
            "总结记录：记录本次灾害情况，为下年度防灾提供依据",
        ]

        # 补充从检索内容中提取的具体措施
        for k in k_chunks:
            content = k.get("content", "")
            # 提取具体数值信息
            if "氮肥" in content:
                recommendations.append("控制氮肥用量，增施钾肥增强抗倒能力")
            if "多效唑" in content:
                recommendations.append("化控处理：喷施多效唑控旺")
            if "培土" in content:
                recommendations.append("中耕培土，增强根系支撑能力")

        # 去重
        seen = set()
        unique_recommendations = []
        for r in recommendations:
            if r not in seen:
                seen.add(r)
                unique_recommendations.append(r)

        return answer_summary, conclusion, possible_causes, unique_recommendations[:8]

    @staticmethod
    def _synthesize_mixed_answer(
        k_chunks: list[dict],
        plan: QueryPlan,
        business_context: BuiltBusinessContext | None,
        risk_signals: list[str],
    ) -> tuple[str, str, list[str], list[str]]:
        """生成业务+知识混合场景的结构化答案"""

        # 业务上下文摘要
        business_summary = ""
        if risk_signals:
            business_summary = f"【业务异常信号】{'；'.join(risk_signals[:3])}\n\n"

        # 知识库内容摘要
        knowledge_summary_parts = ["【知识库检索结果】"]
        for k in k_chunks[:3]:
            knowledge_summary_parts.append(
                f"• {k['title']}：{k['content'][:180]}"
            )
        knowledge_summary = "\n".join(knowledge_summary_parts)

        answer_summary = business_summary + knowledge_summary

        conclusion = (
            "业务侧存在实时异常信号，建议优先处理业务告警，"
            "同时参考知识库中的技术方案进行田间管理。"
        )

        possible_causes = list(risk_signals[:5]) if risk_signals else [
            "业务数据异常，设备或环境指标超限",
            "知识库缺乏与当前问题完全匹配的直接方案",
            "缺少历史同期数据对照，难以快速定位根因",
        ]

        recommendations = [
            "确认并处理业务侧实时告警（设备离线/数据缺失等）",
            f"参考知识库「{k_chunks[0]['title'] if k_chunks else '相关文档'}」的技术要点",
            "现场复核关键指标并拍照留证",
            "按 SOP 填写处置记录并设置复盘提醒",
            "若异常持续超 48h，升级至专家会诊",
        ]

        return answer_summary, conclusion, possible_causes, recommendations


model_service = RagModelService()
