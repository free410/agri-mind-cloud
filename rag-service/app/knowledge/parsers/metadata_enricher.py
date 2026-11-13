from __future__ import annotations

from app.schemas.crawl import CrawlSourceConfig


CROP_KEYWORDS: dict[str, list[str]] = {
    "番茄": ["番茄", "西红柿"],
    "草莓": ["草莓"],
    "葡萄": ["葡萄"],
    "水稻": ["水稻", "稻田"],
    "小麦": ["小麦"],
    "玉米": ["玉米"],
    "柑橘": ["柑橘", "橘", "橙"],
}

WARNING_KEYWORDS: dict[str, list[str]] = {
    "soil_moisture_low": ["土壤湿度", "墒情", "缺水", "干旱"],
    "temperature_high": ["高温", "温度过高", "热害"],
    "temperature_low": ["低温", "温度过低", "寒潮", "冻害"],
    "light_abnormal": ["光照", "寡照", "辐射"],
    "device_offline": ["设备离线", "设备故障", "停机", "巡检"],
    "pest_disease": ["病虫害", "虫害", "疫病", "蚜虫", "霜霉病"],
}


def detect_crop_type(text: str) -> str | None:
    for crop_type, keywords in CROP_KEYWORDS.items():
        if any(keyword in text for keyword in keywords):
            return crop_type
    return None


def detect_warning_type(text: str) -> str | None:
    for warning_type, keywords in WARNING_KEYWORDS.items():
        if any(keyword in text for keyword in keywords):
            return warning_type
    return None


def infer_topic(source: CrawlSourceConfig, text: str) -> str:
    """优先识别作物/水肥管理类，减少将综合新闻误判为 warning。"""
    if any(k in text for k in ["水稻", "小麦", "玉米", "大豆", "分蘖", "育秧", "移栽", "抽穗", "灌浆", "穗肥"]):
        if any(k in text for k in ["灌溉", "水层", "搁田", "晒田", "排水", "灌水", "淹水", "节水"]):
            return "irrigation"
        return "crop"
    if any(k in text for k in ["灌溉", "水肥", "墒情", "水层", "节水", "灌水"]):
        return "irrigation"
    if any(k in text for k in ["温室", "传感器", "农机", "设备", "烘干"]):
        return "device"
    if any(k in text for k in ["湿度", "大气湿度"]) and "土壤" not in text:
        return "humidity"
    if any(k in text for k in ["病虫害", "防治", "农药", "疫病", "虫害"]):
        return "warning"
    if any(k in text for k in ["种植", "栽培", "田间管理", "施肥", "品种"]):
        return "crop"
    return source.topic
