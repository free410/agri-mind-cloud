"""
知识文本清洗：用于本地文档与抓取结果入库前统一去噪，减轻「新闻体」对向量检索的干扰。
"""

from __future__ import annotations

import re

from app.knowledge.parsers.text_utils import normalize_text

# 常见页脚、转载说明、编辑信息（整行剔除）
_BOILERPLATE_LINE = re.compile(
    r"^\s*("
    r"责任编辑|编辑[:：]|来源[:：]|文章来源|本文转自|转载请注明|投稿邮箱|"
    r"扫描.*二维码|微信扫一扫|分享到|相关阅读|延伸阅读|推荐阅读|"
    r"上一篇|下一篇|返回列表|打印本页|关闭窗口|"
    r"Copyright|版权所有|ICP备"
    r")\b",
    re.IGNORECASE,
)

# 纯 URL 行、仅符号行
_JUNK_LINE = re.compile(r"^\s*(https?://\S+|www\.\S+)\s*$")
_SYMBOL_ONLY = re.compile(r"^[\s\-_=·•\*#|/\\，。、:：;；]+$")

# 残留 HTML 标签
_HTML_TAG = re.compile(r"<[^>]+>")

# 农技相关词（用于抓取结果过滤打分，命中越多越可能保留）
_AGRONOMY_KEYWORDS: tuple[str, ...] = (
    "水稻",
    "小麦",
    "玉米",
    "大豆",
    "油菜",
    "棉花",
    "分蘖",
    "拔节",
    "孕穗",
    "抽穗",
    "灌浆",
    "成熟",
    "育秧",
    "移栽",
    "直播",
    "水层",
    "灌溉",
    "搁田",
    "晒田",
    "排水",
    "灌水",
    "淹水",
    "节水",
    "墒情",
    "水肥",
    "施肥",
    "追肥",
    "穗肥",
    "基肥",
    "病虫害",
    "防治",
    "农药",
    "栽培",
    "种植",
    "田间",
    "农艺",
    "农技",
    "品种",
    "选种",
    "密植",
    "温室",
    "大棚",
    "土壤",
    "有机肥",
    "番茄",
    "草莓",
    "黄瓜",
    "辣椒",
    "苹果",
    "葡萄",
    "柑橘",
    "马铃薯",
    "病害",
    "虫害",
    "疫病",
    "霜霉病",
    "灰霉病",
    "晚疫病",
    "早疫病",
    "稻瘟病",
    "纹枯病",
    "螟虫",
    "红蜘蛛",
    "土传",
    "轮作",
    "深耕",
    "育苗",
    "测土配方",
    "节水灌溉",
)

# 明显非生产技术向标题关键词（整篇剔除，避免党建/会议类进库）
_NOISE_TITLE_MARKERS: tuple[str, ...] = (
    "党建",
    "理论学习",
    "座谈会",
    "研讨会",
    "调研",
    "考察",
    "禁渔",
    "执法",
    "颁奖",
    "表彰大会",
    "动员会",
    "精神",
    "学习贯彻",
)


def strip_html_fragments(text: str) -> str:
    t = _HTML_TAG.sub(" ", text)
    return t


def clean_for_index(text: str) -> str:
    """入库前清洗：去 HTML 碎片、按行去噪、归一化空白。"""
    t = strip_html_fragments(text)
    t = normalize_text(t)
    lines_out: list[str] = []
    for line in t.split("\n"):
        line = line.strip()
        if not line or len(line) < 2:
            continue
        if _JUNK_LINE.match(line) or _SYMBOL_ONLY.match(line):
            continue
        if _BOILERPLATE_LINE.match(line):
            continue
        lines_out.append(line)
    merged = "\n\n".join(lines_out)
    merged = re.sub(r"\n{3,}", "\n\n", merged)
    return normalize_text(merged)


def agronomic_keyword_score(text: str) -> int:
    """合并标题+正文，统计农技词命中种类数（每种词最多计 1 次）。"""
    if not text:
        return 0
    seen = 0
    for kw in _AGRONOMY_KEYWORDS:
        if kw in text:
            seen += 1
    return seen


def is_noise_title(title: str) -> bool:
    t = (title or "").strip()
    if not t:
        return True
    return any(m in t for m in _NOISE_TITLE_MARKERS)
