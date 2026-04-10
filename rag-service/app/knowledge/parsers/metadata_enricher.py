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
    if any(keyword in text for keyword in ["病虫害", "防治", "农药"]):
        return "warning"
    if any(keyword in text for keyword in ["灌溉", "水肥", "墒情"]):
        return "irrigation"
    if any(keyword in text for keyword in ["温室", "设备", "传感器", "农机"]):
        return "device"
    if any(keyword in text for keyword in ["种植", "栽培", "田间管理"]):
        return "crop"
    return source.topic
