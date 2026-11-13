"""
农业领域RAG Prompt构建器：针对作物倒伏、病虫害等紧急农业问题深度优化。
"""

import logging
import re
from typing import Literal

from app.schemas.rag import BuiltBusinessContext, QueryPlan, RagReferenceItem

logger = logging.getLogger(__name__)


class RagPromptBuilder:
    """
    重构后的Prompt构建器，针对农业紧急问题（倒伏、病虫害）深度优化。
    """

    # 检测是否为紧急农业问题
    URGENT_PATTERNS = [
        r"倒伏|伏倒",
        r"减产|绝收|歉收",
        r"死棵|死苗|枯萎",
        r"病虫害爆发|大面积发病",
        r"补救|救治|抢救",
    ]

    def is_urgent_agriculture_issue(self, query: str) -> bool:
        """判断是否为紧急农业问题"""
        return any(re.search(p, query) for p in self.URGENT_PATTERNS)

    def build(
        self,
        user_query: str,
        plan: QueryPlan,
        business_context: BuiltBusinessContext | None,
        knowledge_refs: list[RagReferenceItem],
        knowledge_graph_context: str | None = None,
        intent_domain: str | None = None,
        relevant_scores: list[float] | None = None,
    ) -> str:
        """
        构建完整的Prompt。
        """
        sections: list[str] = []

        # ========== 系统角色与约束 ==========
        sections.append(self._build_system_constraints(intent_domain))

        # ========== 用户问题 ==========
        sections.append(f"[用户问题] {user_query}")

        # ========== 业务上下文 ==========
        if business_context and business_context.has_business_data:
            sections.append("[业务上下文摘要]")
            sections.extend(f"- {item}" for item in business_context.summary_blocks[:12])
            sections.append("[业务上下文详情]")
            sections.append(business_context.context_text or "无")
            sections.append("[风险信号]")
            sections.extend(f"- {signal}" for signal in business_context.risk_signals[:12])
        else:
            sections.append("[业务上下文] 无可用实时业务数据。")

        # ========== 知识检索片段（核心） ==========
        sections.append(self._build_knowledge_section(knowledge_refs, relevant_scores, user_query))

        # ========== 知识图谱关联 ==========
        if knowledge_graph_context and knowledge_graph_context.strip():
            sections.append("[知识图谱关联]")
            sections.append(knowledge_graph_context.strip())
        else:
            sections.append("[知识图谱关联] 无。")

        # ========== 回答格式与质量要求 ==========
        sections.append(self._build_output_requirements(user_query, intent_domain))

        return "\n".join(sections)

    def _build_system_constraints(self, intent_domain: str | None = None) -> str:
        """构建系统约束部分"""
        return """【系统角色】你是智慧农业技术顾问，专注于作物栽培、病虫害防治、灾后补救。

【硬性约束 - 必须遵守】
1. 【只回答技术问题】你只能回答关于作物种植、病虫害、施肥、灌溉、灾后补救等技术问题
2. 【禁止回答价格/行情】严禁回答农产品价格、市场行情、金融政策、新闻资讯等内容
3. 【禁止无中生有】严禁编造农药名称、肥料配比、具体剂量等数据
4. 【强制专业】使用规范的农业术语，如"倒伏"而非"倒了"，"茎腐"而非"烂茎"
5. 【禁止混淆】玉米价格、小麦政策等与种植技术无关的问题，必须明确告知"请咨询价格/政策相关渠道"

【当前领域】"""

    def _build_knowledge_section(
        self,
        knowledge_refs: list[RagReferenceItem],
        relevant_scores: list[float] | None,
        user_query: str,
    ) -> str:
        """构建知识检索片段部分"""
        if not knowledge_refs:
            return "[知识检索片段] 警告：未检索到任何相关知识！"

        sections = ["[知识检索片段 - 务必严格基于以下内容回答]"]

        for idx, ref in enumerate(knowledge_refs[:5]):
            score = relevant_scores[idx] if relevant_scores and idx < len(relevant_scores) else ref.score
            domain = ref.metadata.get("domain", "unknown")

            # 检查是否为噪音内容
            noise_score = self._check_noise_content(ref.content, user_query)
            if noise_score > 0:
                sections.append(
                    f"{idx + 1}. [警告-可能不相关 domain={domain} 分={score:.4f}] {ref.title}\n   {ref.content[:400]}"
                )
            else:
                sections.append(
                    f"{idx + 1}. [高相关 domain={domain} 分={score:.4f}] {ref.title}\n   {ref.content[:500]}"
                )

        return "\n".join(sections)

    def _check_noise_content(self, content: str, query: str) -> float:
        """
        检测内容是否为噪音（价格/新闻等无关内容）

        Returns:
            噪音分数，0表示是相关内容
        """
        noise_keywords = {
            "价格类": ["元/斤", "元/公斤", "批发价", "零售价", "价格指数", "市场价", "涨跌"],
            "政策类": ["补贴政策", "惠农", "扶持资金", "贷款", "保险"],
            "新闻类": ["记者", "报道", "据悉", "日前", "农业部", "统计局"],
        }

        noise_score = 0.0
        for noise_type, keywords in noise_keywords.items():
            for kw in keywords:
                if kw in content:
                    # 检查query中是否也有这个词（如果是问价格则不算噪音）
                    if kw not in query:
                        noise_score += 0.5

        return noise_score

    def _build_output_requirements(
        self,
        user_query: str,
        intent_domain: str | None = None,
    ) -> str:
        """构建输出格式与质量要求"""

        is_lodging = "倒伏" in user_query
        is_crop_tech = intent_domain == "crop_disease"

        requirements = [
            "[输出要求 - 必须严格遵守]",
            "",
            "【answer_summary】综合分析，必须包含：",
            "  - 问题诊断（如：这是玉米茎倒伏，属于机械损伤）",
            "  - 原因分析（如：暴雨+根系浅是主因）",
            "  - 紧迫性评估（轻度/中度/严重）",
            "",
            "【conclusion】给出明确结论和优先级最高的行动建议",
            "",
            "【possible_causes】至少4条可能原因，从以下角度分析：",
            "  - 气象因素（暴雨、大风、台风）",
            "  - 土壤因素（土壤松软、根系浅）",
            "  - 栽培因素（种植密度过大、氮肥过量）",
            "  - 品种因素（抗倒伏性差的品种）",
            "",
            "【recommendations】至少5条具体措施，按紧急程度排序：",
            "  1. 立即措施（24小时内）",
            "  2. 短期措施（3-7天）",
            "  3. 长期预防措施",
        ]

        # 针对倒伏问题的特殊要求
        if is_lodging:
            requirements.extend([
                "",
                "[玉米倒伏专项要求 - 必须全部覆盖]",
                "",
                "■ 1. 倒伏程度判断",
                "   - 轻度倒伏：茎秆与地面夹角>45度，根系未断",
                "   - 中度倒伏：茎秆与地面夹角30-45度，部分根系受损",
                "   - 严重倒伏：茎秆贴地，根系断裂或茎折",
                "",
                "■ 2. 补救措施（按倒伏程度）",
                "   轻度：人工扶直，扎把固定，喷施叶面肥",
                "   中度：不要强行扶直，避免茎秆折断，加强病虫害防治",
                "   严重（茎折）：放弃扶直，及时收获做青贮，减少损失",
                "",
                "■ 3. 后续田间管理",
                "   - 排水降渍：及时排除田间积水",
                "   - 追施肥料：每亩施尿素5-8公斤+氯化钾3-5公斤",
                "   - 病虫害防控：重点防治玉米螟、纹枯病",
                "",
                "■ 4. 注意事项",
                "   - 扶直应在倒伏后3天内、茎秆未完全木质化前完成",
                "   - 扶直时避免伤根，力度要轻",
                "   - 田间作业注意安全，防止二次伤害",
                "",
                "■ 5. 产量损失评估",
                "   - 轻度倒伏：产量损失10-20%",
                "   - 中度倒伏：产量损失30-50%",
                "   - 严重倒伏：产量损失可达80%以上",
            ])

        # 强制约束
        requirements.extend([
            "",
            "[强制约束 - 违反将导致错误答案]",
            "1. 禁止使用价格、行情、指数等非技术内容回答",
            "2. 禁止编造农药商品名、具体剂量",
            "3. 如知识片段与问题无关，必须明确说明「未找到相关知识」",
            "4. 答案必须专业、准确、可执行",
        ])

        requirements.append("")
        requirements.append("请返回JSON格式（必须包含所有字段）：")
        requirements.append('{answer_summary, conclusion, possible_causes, recommendations, priority, should_manual_review}')

        return "\n".join(requirements)

    def build_fallback_prompt(self, user_query: str, intent_domain: str | None = None) -> str:
        """
        构建无检索结果时的兜底Prompt。

        【关键改进】即使没有检索到内容，也要基于农业通用知识给出专业建议！
        """
        is_lodging = "倒伏" in user_query

        prompt_parts = [
            "【系统角色】智慧农业技术顾问",
            "",
            "【用户问题】" + user_query,
            "",
            "[重要] 虽然未检索到精确匹配的知识，但请基于农业通用知识给出专业回答。",
        ]

        if is_lodging:
            prompt_parts.extend([
                "",
                "[玉米倒伏 - 通用补救知识]",
                "",
                "一、倒伏程度判断",
                "1. 轻度（根倒）：玉米植株从根部倾斜，茎秆未折断，仍有恢复可能",
                "2. 重度（茎折）：玉米茎秆从中间折断，无法恢复",
                "",
                "二、轻度倒伏补救措施",
                "1. 人工扶直：在倒伏后2-3天内进行，用木棍将玉米植株轻轻扶起",
                "2. 扎把固定：用细绳将3-5株玉米扎在一起，互相支撑",
                "3. 叶面施肥：喷施0.3%磷酸二氢钾+0.2%尿素溶液，促进灌浆",
                "",
                "三、重度倒伏（茎折）处理",
                "1. 茎折的植株无法补救，应及时清除",
                "2. 可提前收获做青贮饲料，减少损失",
                "",
                "四、灾后管理要点",
                "1. 及时排水，防止根系腐烂",
                "2. 追施氮肥，每亩5-8公斤，促进恢复",
                "3. 加强病虫害监测，重点防治玉米螟",
                "4. 为下季作物补充钾肥，增强抗倒能力",
                "",
                "五、下年度预防措施",
                "1. 选择抗倒伏品种",
                "2. 合理密植，密度过大易倒伏",
                "3. 科学施肥，避免氮肥过量",
                "4. 中耕培土，增强根系支撑",
                "5. 拔节期喷施玉米健壮素控高",
            ])
        else:
            prompt_parts.extend([
                "",
                "[农业通用知识]",
                "请基于作物栽培、病虫害防治的通用知识给出专业建议。",
            ])

        prompt_parts.extend([
            "",
            "[输出要求] 必须返回JSON格式：",
            '{answer_summary, conclusion, possible_causes, recommendations, priority, should_manual_review}',
        ])

        return "\n".join(prompt_parts)


prompt_builder = RagPromptBuilder()
