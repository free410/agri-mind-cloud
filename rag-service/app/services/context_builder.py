from __future__ import annotations

from app.schemas.rag import BuiltBusinessContext, BusinessContextPayload


class BusinessContextBuilder:
    WARNING_KEYWORDS = (
        "预警",
        "告警",
        "闭环",
        "处置",
        "误报",
        "设备离线",
        "土壤湿度",
        "温度",
        "光照",
        "巡检",
    )
    KNOWLEDGE_KEYWORDS = (
        "是什么",
        "为什么",
        "怎么做",
        "如何",
        "原理",
        "SOP",
        "病虫害",
        "栽培",
        "指南",
    )

    def classify_question(self, user_query: str) -> str:
        query = user_query.strip()
        warning_hit = any(keyword in query for keyword in self.WARNING_KEYWORDS)
        knowledge_hit = any(keyword in query for keyword in self.KNOWLEDGE_KEYWORDS)
        if warning_hit and knowledge_hit:
            return "hybrid"
        if warning_hit:
            return "warning"
        if knowledge_hit:
            return "knowledge"
        return "hybrid"

    def build(self, question_type: str, payload: BusinessContextPayload | None) -> BuiltBusinessContext:
        if payload is None:
            return BuiltBusinessContext(
                question_type=question_type,  # type: ignore[arg-type]
                has_business_data=False,
                context_text="",
                risk_signals=[],
                summary_blocks=[],
                raw=None,
            )

        summary_blocks: list[str] = []
        risk_signals: list[str] = []
        lines: list[str] = []

        if payload.current_warning:
            warning = payload.current_warning
            summary_blocks.append(
                f"当前预警：{warning.warning_type or '未知类型'} / {warning.level or '未知等级'} / {warning.status or '未知状态'}"
            )
            lines.append(
                f"[当前预警] 类型={warning.warning_type} 等级={warning.level} 状态={warning.status} 内容={warning.content}"
            )
            if warning.level in ("高", "HIGH"):
                risk_signals.append("存在高等级预警，建议优先处置")

        if payload.recent_env_data:
            latest = payload.recent_env_data[0]
            summary_blocks.append(
                f"最近环境：温度{self._fmt(latest.temperature)}℃ 湿度{self._fmt(latest.humidity)}% "
                f"土壤湿度{self._fmt(latest.soil_moisture)}%"
            )
            lines.append(
                f"[环境快照] 温度={latest.temperature} 湿度={latest.humidity} 土壤湿度={latest.soil_moisture} "
                f"光照={latest.light_intensity} pH={latest.ph_value} 采集时间={latest.collect_time}"
            )
            if latest.soil_moisture is not None and latest.soil_moisture < 35:
                risk_signals.append("土壤湿度偏低")
            if latest.temperature is not None and latest.temperature > 35:
                risk_signals.append("温度偏高")
            if latest.temperature is not None and latest.temperature < 5:
                risk_signals.append("温度偏低")

        if payload.related_devices:
            offline_count = sum(1 for device in payload.related_devices if device.online_status == 0)
            summary_blocks.append(f"关联设备：{len(payload.related_devices)} 台（离线 {offline_count} 台）")
            lines.append(
                f"[设备状态] 总数={len(payload.related_devices)} 离线={offline_count} "
                + "; ".join(
                    f"{item.device_name or item.device_code}:{'在线' if item.online_status == 1 else '离线'}"
                    for item in payload.related_devices[:5]
                )
            )
            if offline_count > 0:
                risk_signals.append("存在离线设备，需要巡检")

        if payload.related_farmland:
            field = payload.related_farmland
            summary_blocks.append(f"所属农田：{field.field_name or '-'}")
            lines.append(
                f"[农田信息] 名称={field.field_name} 位置={field.location} 土壤类型={field.soil_type} 状态={field.status}"
            )

        if payload.related_crops:
            summary_blocks.append(f"关联作物：{len(payload.related_crops)} 项")
            lines.append(
                "[作物信息] "
                + "; ".join(
                    f"{crop.crop_name or crop.crop_type}/{crop.growth_stage}/{crop.health_status}"
                    for crop in payload.related_crops[:5]
                )
            )

        if payload.process_logs:
            latest_log = payload.process_logs[0]
            summary_blocks.append(f"最新处理：{latest_log.action or '-'}（{latest_log.operation_time or '-'}）")
            lines.append(
                f"[处理记录] 条数={len(payload.process_logs)} 最新动作={latest_log.action} "
                f"状态流转={latest_log.from_status}->{latest_log.to_status} 操作人={latest_log.operator_name}"
            )

        if payload.ai_analyses:
            latest_ai = payload.ai_analyses[0]
            summary_blocks.append(f"AI结论：{latest_ai.priority or '中'}优先级")
            lines.append(
                f"[历史AI分析] 优先级={latest_ai.priority} 需人工复核={latest_ai.need_manual_review} "
                f"摘要={latest_ai.summary} 建议={latest_ai.suggestions}"
            )

        if not risk_signals:
            risk_signals.append("当前未识别到显著新增风险")

        return BuiltBusinessContext(
            question_type=question_type,  # type: ignore[arg-type]
            has_business_data=True,
            context_text="\n".join(lines),
            risk_signals=risk_signals,
            summary_blocks=summary_blocks,
            raw=payload,
        )

    @staticmethod
    def _fmt(value: float | None) -> str:
        if value is None:
            return "-"
        return f"{value:.1f}"


business_context_builder = BusinessContextBuilder()
