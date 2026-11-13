"""
意图路由模块：基于规则 + 轻量关键词匹配判断用户问题领域，
自动路由到对应的知识库子库（crop_disease / market_price / agri_policy）。
"""

import logging
import re
from dataclasses import dataclass
from enum import Enum
from typing import Literal

logger = logging.getLogger(__name__)


class KnowledgeDomain(str, Enum):
    """知识库领域枚举，对应三个独立向量子库"""
    CROP_DISEASE = "crop_disease"       # 作物种植、病虫害、施肥等技术知识
    MARKET_PRICE = "market_price"       # 农产品价格、行情、成本收益
    AGRI_POLICY = "agri_policy"          # 农业政策、补贴、贷款
    GENERAL = "general"                 # 通用农业问题，不限定子库


@dataclass
class RouteResult:
    domain: KnowledgeDomain
    confidence: float  # 0.0 ~ 1.0，置信度
    matched_keywords: list[str]
    reasoning: str


class IntentRouter:
    """
    基于规则的意图分类器，用于判断用户问题属于哪个农业知识领域。

    路由规则：
    - crop_disease：病害、黄叶、斑点、防治、施肥、种植、浇水、病虫害等
    - market_price：价格、行情、成本、收益、批发、零售、市场等
    - agri_policy：补贴、政策、贷款、扶持、农业保险、免税等
    - general：其他通用农业问题
    """

    # 领域关键词配置
    DOMAIN_PATTERNS: dict[KnowledgeDomain, list[tuple[str, float]]] = {
        KnowledgeDomain.CROP_DISEASE: [
            # ========== 【核心农业技术关键词】高权重 ==========
            # 灾害/损伤类（倒伏是典型问题）
            (r"倒伏|伏倒|倾斜|弯折|折断|茎折", 0.95),
            # 产量相关
            (r"减产|绝收|歉收|产量低|产量下降|产量损失|死棵|死苗|缺苗", 0.95),
            # 病害相关
            (r"病|病害|发病|病毒|真菌|细菌|霉病|枯萎|腐烂|斑点|叶斑|黄化|黄叶|叶黄|卷叶|萎蔫", 0.9),
            # 虫害相关
            (r"虫|蚜虫|螨虫|螟虫|蛾|蛴螬|地老虎|粉虱|蓟马|红蜘蛛|虫害|害虫|防虫|杀虫|除虫", 0.85),
            # 生长问题
            (r"不分蘖|分蘖少|不长|生长慢|烂根|烧根|徒长|早熟|晚熟|畸形|裂果|空心|坐果|落花|落果", 0.9),
            # 营养缺乏
            (r"缺[氮磷钾钙镁铁锌硼]|缺素|营养不良|叶片发黄|叶色异常|叶缘焦枯|叶尖干枯", 0.9),
            # 种植管理
            (r"种植|栽培|育苗|移栽|播种|施肥|追肥|底肥|有机肥|化肥|复合肥|氮肥|磷肥|钾肥|浇水|灌溉|排水|整地", 0.8),
            # 农药/补救相关
            (r"农药|打药|喷药|喷雾|杀菌剂|杀虫剂|除草剂|药害|配药|浓度|补救|救治|抢救|修复", 0.85),
            # ========== 【作物名称】辅助权重 ==========
            (r"玉米|小麦|水稻|黄瓜|番茄|茄子|辣椒|白菜|萝卜|土豆|大豆|棉花|油菜|苹果|梨|葡萄|草莓", 0.25),
        ],
        KnowledgeDomain.MARKET_PRICE: [
            # 价格相关（严格区分）
            (r"价格|报价|价位|价钱|多少钱|涨跌|涨价|跌价|升价|降价|今日价格|最新价格", 0.95),
            # 行情相关
            (r"行情|走势|趋势|市场预测|预期|展望|分析|报告", 0.85),
            # 收益相关
            (r"收益|利润|赚钱|盈利|亏本|成本|收益比|投入产出|回报|赚多少", 0.85),
            # 销售渠道
            (r"销售|卖|收购|批发|零售|渠道|市场|集市|超市|电商|平台", 0.75),
            # 具体价格（严格限定）
            (r"元/[公斤斤]|元/吨|元/亩|每[公斤斤]|每[亩分]|报价单|价格表|指数", 0.9),
        ],
        KnowledgeDomain.AGRI_POLICY: [
            # 补贴政策
            (r"补贴|补助|扶持资金|专项资金|惠农|惠农政策|农业补贴|种粮补贴|农机补贴|地力补贴", 0.9),
            # 政策相关
            (r"政策|法规|条例|规定|标准|规范|国家[农业农村]|中央|省级|县级|农业局|农委", 0.8),
            # 贷款融资
            (r"贷款|融资|信贷|抵押|担保|信用贷款|惠农贷|农担贷|农业贷款|创业贷", 0.85),
            # 保险
            (r"农业保险|保险|参保|理赔|保费补贴|政策性保险|种植险|养殖险", 0.85),
            # 土地流转
            (r"土地流转|承包|租赁|流转|确权|登记|宅基地|耕地保护|流转合同", 0.7),
        ],
    }

    # 反向过滤：包含这些词的绝不能算price/news
    TECH_OVERRIDE_KEYWORDS = [
        "倒伏", "减产", "补救", "救治", "分蘖", "病害", "虫害", "施肥",
        "种植", "栽培", "产量", "死棵", "死苗", "枯萎", "腐烂",
    ]
    MARKET_NOISE_KEYWORDS = [
        "价格", "行情", "指数", "涨跌", "批发价", "零售价", "成本",
        "利润", "收益", "赚钱", "市场价", "今日价格",
    ]

    def __init__(self, min_confidence: float = 0.5):
        """
        Args:
            min_confidence: 最低置信度阈值，低于此值判定为 GENERAL
        """
        self.min_confidence = min_confidence
        # 预编译正则表达式提升性能
        self._compiled_patterns: dict[KnowledgeDomain, list[tuple[re.Pattern, float]]] = {}
        for domain, patterns in self.DOMAIN_PATTERNS.items():
            compiled = [(re.compile(p, re.IGNORECASE), weight) for p, weight in patterns]
            self._compiled_patterns[domain] = compiled

    def route(self, query: str) -> RouteResult:
        """
        判断用户问题应该路由到哪个知识库。

        Args:
            query: 用户问题文本

        Returns:
            RouteResult: 包含领域、置信度、匹配关键词、推理过程
        """
        if not query or not query.strip():
            return RouteResult(
                domain=KnowledgeDomain.GENERAL,
                confidence=0.0,
                matched_keywords=[],
                reasoning="空问题，无法判断领域"
            )

        query_clean = query.strip()

        # ========== 【核心修复】反向过滤逻辑 ==========
        # 如果问题包含农业技术关键词，即使有价格词也优先技术
        has_tech_keyword = any(kw in query_clean for kw in self.TECH_OVERRIDE_KEYWORDS)
        has_price_keyword = any(kw in query_clean for kw in self.MARKET_NOISE_KEYWORDS)

        # 场景1：技术问题（倒伏、减产、病虫害等）优先
        if has_tech_keyword:
            # 计算技术领域得分
            tech_score = self._calc_domain_score(query_clean, KnowledgeDomain.CROP_DISEASE)
            if tech_score >= 0.5:
                matched_kws = self._get_matched_keywords(query_clean, KnowledgeDomain.CROP_DISEASE)
                logger.info(f"[IntentRouter] 技术关键词触发，强制路由到crop_disease query={query_clean[:30]}")
                return RouteResult(
                    domain=KnowledgeDomain.CROP_DISEASE,
                    confidence=min(0.95, 0.6 + tech_score * 0.2),
                    matched_keywords=matched_kws,
                    reasoning=f"检测到农业技术关键词（倒伏/减产/病虫害），优先路由到技术知识库"
                )

        # 场景2：纯价格/行情问题
        # 只有价格词没有技术词，才算价格问题
        if has_price_keyword and not has_tech_keyword:
            market_score = self._calc_domain_score(query_clean, KnowledgeDomain.MARKET_PRICE)
            if market_score >= 0.5:
                matched_kws = self._get_matched_keywords(query_clean, KnowledgeDomain.MARKET_PRICE)
                return RouteResult(
                    domain=KnowledgeDomain.MARKET_PRICE,
                    confidence=min(0.95, 0.6 + market_score * 0.2),
                    matched_keywords=matched_kws,
                    reasoning=f"检测到价格/行情关键词，路由到市场信息库"
                )

        # 场景3：通用分类逻辑
        domain_scores: dict[KnowledgeDomain, tuple[float, list[str]]] = {}

        for domain, compiled_patterns in self._compiled_patterns.items():
            total_weight = 0.0
            matched_kws: list[str] = []

            for pattern, weight in compiled_patterns:
                matches = pattern.findall(query_clean)
                if matches:
                    unique_matches = list(dict.fromkeys(matches))
                    matched_kws.extend(unique_matches)
                    total_weight += weight

            if matched_kws:
                domain_scores[domain] = (total_weight, list(dict.fromkeys(matched_kws)))

        if not domain_scores:
            return RouteResult(
                domain=KnowledgeDomain.GENERAL,
                confidence=0.3,
                matched_keywords=[],
                reasoning="未匹配到任何领域关键词，归类为通用问题"
            )

        # 找到得分最高的领域
        best_domain = max(domain_scores.keys(), key=lambda d: domain_scores[d][0])
        best_score, best_kws = domain_scores[best_domain]

        # 归一化置信度
        if best_score >= 1.6:
            confidence = min(0.95, 0.6 + best_score * 0.15)
        elif best_score >= 1.0:
            confidence = min(0.85, 0.4 + best_score * 0.25)
        else:
            confidence = min(0.7, best_score * 0.5)

        if confidence < self.min_confidence:
            return RouteResult(
                domain=KnowledgeDomain.GENERAL,
                confidence=confidence,
                matched_keywords=best_kws,
                reasoning=f"最高置信度 {confidence:.2f} 低于阈值 {self.min_confidence}，降级为通用问题"
            )

        domain_name_map = {
            KnowledgeDomain.CROP_DISEASE: "作物病害与种植技术",
            KnowledgeDomain.MARKET_PRICE: "农产品价格与市场",
            KnowledgeDomain.AGRI_POLICY: "农业政策与补贴",
            KnowledgeDomain.GENERAL: "通用农业",
        }

        return RouteResult(
            domain=best_domain,
            confidence=confidence,
            matched_keywords=best_kws,
            reasoning=f"匹配到领域「{domain_name_map[best_domain]}」，得分 {best_score:.2f}，置信度 {confidence:.2f}"
        )

    def _calc_domain_score(self, query: str, domain: KnowledgeDomain) -> float:
        """计算特定领域的匹配得分"""
        compiled_patterns = self._compiled_patterns.get(domain, [])
        total = 0.0
        for pattern, weight in compiled_patterns:
            if pattern.search(query):
                total += weight
        return total

    def _get_matched_keywords(self, query: str, domain: KnowledgeDomain) -> list[str]:
        """获取匹配的关键词列表"""
        matched = []
        compiled_patterns = self._compiled_patterns.get(domain, [])
        for pattern, _ in compiled_patterns:
            matches = pattern.findall(query)
            matched.extend(matches)
        return list(dict.fromkeys(matched))

    def get_collection_name(self, domain: KnowledgeDomain) -> str:
        """
        根据领域获取对应的Qdrant集合名称。

        Args:
            domain: 知识领域

        Returns:
            Qdrant集合名称
        """
        collection_map = {
            KnowledgeDomain.CROP_DISEASE: "agriculture_knowledge",
            KnowledgeDomain.MARKET_PRICE: "agriculture_market_price",
            KnowledgeDomain.AGRI_POLICY: "agriculture_agri_policy",
            KnowledgeDomain.GENERAL: "agriculture_knowledge",  # 通用库作为兜底
        }
        return collection_map.get(domain, "agriculture_knowledge")


# 全局单例
intent_router = IntentRouter(min_confidence=0.4)
