# -*- coding: utf-8 -*-
"""生成知识图谱种子 JSON（农学常识三元组）。运行: python generate_agri_graph_seed.py"""
from __future__ import annotations

import json
from pathlib import Path

REF_TECH = "农业农村部-绿色防控与单产提升技术意见"
REF_PEST = "全国农作物重大病虫害监测预报办法（技术摘要）"
REF_SOIL = "测土配方施肥技术指南"
REF_IRR = "农田灌溉水质与节水灌溉技术规范"
REF_FERT = "肥料合理使用准则通则"
REF_ENV = "农业气象灾害风险区划要点"
REF_STORE = "农产品贮藏保鲜技术手册"

# id -> (中文名, kind)
E: dict[str, tuple[str, str]] = {}

def reg(eid: str, name: str, kind: str) -> str:
    E[eid] = (name, kind)
    return eid


# ---------- 作物 ----------
CROPS = [
    ("crop_rice", "水稻", "crop"),
    ("crop_wheat", "小麦", "crop"),
    ("crop_maize", "玉米", "crop"),
    ("crop_soybean", "大豆", "crop"),
    ("crop_potato", "马铃薯", "crop"),
    ("crop_peanut", "花生", "crop"),
    ("crop_rapeseed", "油菜", "crop"),
    ("crop_cotton", "棉花", "crop"),
    ("crop_tomato", "番茄", "crop"),
    ("crop_cucumber", "黄瓜", "crop"),
    ("crop_pepper", "辣椒", "crop"),
    ("crop_eggplant", "茄子", "crop"),
    ("crop_cabbage", "大白菜", "crop"),
    ("crop_broccoli", "西兰花", "crop"),
    ("crop_carrot", "胡萝卜", "crop"),
    ("crop_spinach", "菠菜", "crop"),
    ("crop_lettuce", "生菜", "crop"),
    ("crop_celery", "芹菜", "crop"),
    ("crop_garlic", "大蒜", "crop"),
    ("crop_onion", "洋葱", "crop"),
    ("crop_apple", "苹果", "crop"),
    ("crop_pear", "梨", "crop"),
    ("crop_peach", "桃", "crop"),
    ("crop_grape", "葡萄", "crop"),
    ("crop_citrus", "柑橘", "crop"),
    ("crop_strawberry", "草莓", "crop"),
    ("crop_watermelon", "西瓜", "crop"),
    ("crop_melon", "甜瓜", "crop"),
]
for cid, cname, k in CROPS:
    reg(cid, cname, k)

# ---------- 病害 ----------
DISEASES = [
    ("dis_rice_blast", "稻瘟病", "disease"),
    ("dis_rice_sheath_blight", "水稻纹枯病", "disease"),
    ("dis_rice_bacterial_leaf", "水稻白叶枯病", "disease"),
    ("dis_wheat_rust", "小麦锈病", "disease"),
    ("dis_wheat_scab", "小麦赤霉病", "disease"),
    ("dis_wheat_powdery", "小麦白粉病", "disease"),
    ("dis_maize_leaf_blight", "玉米大斑病", "disease"),
    ("dis_maize_stalk_rot", "玉米茎腐病", "disease"),
    ("dis_potato_late_blight", "马铃薯晚疫病", "disease"),
    ("dis_potato_early_blight", "马铃薯早疫病", "disease"),
    ("dis_tomato_late_blight", "番茄晚疫病", "disease"),
    ("dis_tomato_early_blight", "番茄早疫病", "disease"),
    ("dis_tomato_leaf_mold", "番茄叶霉病", "disease"),
    ("dis_gray_mold", "灰霉病", "disease"),
    ("dis_cucumber_downy", "黄瓜霜霉病", "disease"),
    ("dis_cucumber_powdery", "黄瓜白粉病", "disease"),
    ("dis_cucumber_wilt", "黄瓜枯萎病", "disease"),
    ("dis_pepper_phytophthora", "辣椒疫病", "disease"),
    ("dis_pepper_anthracnose", "辣椒炭疽病", "disease"),
    ("dis_apple_canker", "苹果腐烂病", "disease"),
    ("dis_apple_ring_rot", "苹果轮纹病", "disease"),
    ("dis_apple_powdery", "苹果白粉病", "disease"),
    ("dis_citrus_canker", "柑橘溃疡病", "disease"),
    ("dis_citrus_sooty", "柑橘煤烟病", "disease"),
    ("dis_grape_downy", "葡萄霜霉病", "disease"),
    ("dis_grape_powdery", "葡萄白粉病", "disease"),
    ("dis_strawberry_powdery", "草莓白粉病", "disease"),
    ("dis_strawberry_gray", "草莓灰霉病", "disease"),
    ("dis_root_rot", "根腐病", "disease"),
    ("dis_virus_complex", "病毒病", "disease"),
]
for x in DISEASES:
    reg(*x)

# ---------- 虫害 ----------
PESTS = [
    ("pest_rice_planthopper", "稻飞虱", "pest"),
    ("pest_rice_stem_borer", "二化螟", "pest"),
    ("pest_aphid", "蚜虫", "pest"),
    ("pest_whitefly", "烟粉虱", "pest"),
    ("pest_thrips", "蓟马", "pest"),
    ("pest_spider_mite", "红蜘蛛", "pest"),
    ("pest_cotton_bollworm", "棉铃虫", "pest"),
    ("pest_armyworm", "草地贪夜蛾", "pest"),
    ("pest_dbm", "小菜蛾", "pest"),
    ("pest_cabbage_worm", "菜青虫", "pest"),
    ("pest_leafminer", "潜叶蝇", "pest"),
    ("pest_nematode", "根结线虫", "pest"),
    ("pest_corn_borer", "玉米螟", "pest"),
    ("pest_fruit_fly", "实蝇类", "pest"),
]
for x in PESTS:
    reg(*x)

# ---------- 逆境 / 环境 / 土壤 ----------
STRESS = [
    ("stress_drought", "干旱胁迫", "stress"),
    ("stress_waterlogging", "涝渍胁迫", "stress"),
    ("stress_salinity", "盐胁迫", "stress"),
    ("stress_low_temp", "低温冷害", "stress"),
    ("stress_high_temp", "高温热害", "stress"),
    ("stress_low_light", "弱光胁迫", "stress"),
    ("stress_soil_moisture_low", "土壤湿度偏低", "stress"),
    ("risk_water_stress", "作物水分胁迫", "concept"),
]
for x in STRESS:
    reg(*x)

ENV = [
    ("env_high_humidity", "高湿环境", "environment"),
    ("env_continuous_rain", "持续阴雨", "environment"),
    ("env_dense_planting", "密植郁闭", "environment"),
    ("env_poor_ventilation", "通风不良", "environment"),
    ("env_heavy_dew", "结露重", "environment"),
]
for x in ENV:
    reg(*x)

SOIL = [
    ("soil_sandy", "砂质土", "soil"),
    ("soil_clay", "黏土", "soil"),
    ("soil_loam", "壤土", "soil"),
    ("soil_acid", "酸性土", "soil"),
    ("soil_alkaline", "碱性土", "soil"),
    ("soil_low_organic", "有机质偏低", "soil"),
]
for x in SOIL:
    reg(*x)

NUTRIENTS = [
    ("nut_n", "氮素", "nutrient"),
    ("nut_p", "磷素", "nutrient"),
    ("nut_k", "钾素", "nutrient"),
    ("nut_ca", "钙素", "nutrient"),
    ("nut_mg", "镁素", "nutrient"),
    ("nut_b", "硼素", "nutrient"),
    ("nut_zn", "锌素", "nutrient"),
    ("nut_fe", "铁素", "nutrient"),
    ("nut_mn", "锰素", "nutrient"),
    ("nut_s", "硫素", "nutrient"),
]
for x in NUTRIENTS:
    reg(*x)

DEF = [
    ("def_n", "缺氮", "deficiency"),
    ("def_p", "缺磷", "deficiency"),
    ("def_k", "缺钾", "deficiency"),
    ("def_ca", "缺钙", "deficiency"),
    ("def_mg", "缺镁", "deficiency"),
    ("def_b", "缺硼", "deficiency"),
    ("def_zn", "缺锌", "deficiency"),
]
for x in DEF:
    reg(*x)

PRACTICES = [
    ("prac_irrigation", "灌溉补水", "practice"),
    ("prac_drip", "滴灌", "practice"),
    ("prac_sprinkler", "喷灌", "practice"),
    ("prac_drainage", "开沟排渍", "practice"),
    ("prac_mulch", "地膜覆盖", "practice"),
    ("prac_rotation", "轮作", "practice"),
    ("prac_deep_till", "深松整地", "practice"),
    ("prac_formula_fert", "测土配方施肥", "practice"),
    ("prac_ipm", "病虫害统防统治", "practice"),
    ("prac_yellow_sticky", "黄板诱杀", "practice"),
    ("prac_bio_control", "生物防治", "practice"),
    ("prac_prune", "整枝疏叶", "practice"),
    ("prac_ventilation", "通风降湿", "practice"),
    ("prac_resistant_var", "选用抗病品种", "practice"),
    ("prac_seed_treat", "种子处理", "practice"),
    ("prac_soil_test", "土壤检测", "practice"),
    ("prac_compost", "有机肥还田", "practice"),
    ("prac_greenhouse_manage", "温湿度调控", "practice"),
]
for x in PRACTICES:
    reg(*x)

FERT = [
    ("fert_urea", "尿素", "fertilizer"),
    ("fert_dap", "磷酸二铵", "fertilizer"),
    ("fert_kcl", "氯化钾", "fertilizer"),
    ("fert_compound", "复合肥", "fertilizer"),
    ("fert_lime", "石灰", "fertilizer"),
    ("fert_borate", "硼肥", "fertilizer"),
    ("fert_zn_sulfate", "硫酸锌", "fertilizer"),
]
for x in FERT:
    reg(*x)

PESTICIDE = [
    ("chem_fungicide", "杀菌剂（合理使用）", "pesticide"),
    ("chem_insecticide", "杀虫剂（合理使用）", "pesticide"),
]
for x in PESTICIDE:
    reg(*x)

GROWTH = [
    ("stage_seedling", "苗期", "growth_stage"),
    ("stage_flowering", "开花期", "growth_stage"),
    ("stage_fruit", "果实膨大期", "growth_stage"),
    ("stage_grain_fill", "灌浆期", "growth_stage"),
]
for x in GROWTH:
    reg(*x)

SYMPTOM = [
    ("sym_yellow_leaf", "叶片黄化", "symptom"),
    ("sym_spot", "病斑", "symptom"),
    ("sym_wilt", "萎蔫", "symptom"),
    ("sym_mold", "霉层", "symptom"),
]
for x in SYMPTOM:
    reg(*x)

edges: list[tuple[str, str, str, str, float | None]] = []

def e(h: str, t: str, pred: str, ref: str, conf: float | None = None):
    edges.append((h, t, pred, ref, conf))


# 作物-病害（真实寄主关系）
CROP_DIS = {
    "crop_rice": ["dis_rice_blast", "dis_rice_sheath_blight", "dis_rice_bacterial_leaf"],
    "crop_wheat": ["dis_wheat_rust", "dis_wheat_scab", "dis_wheat_powdery"],
    "crop_maize": ["dis_maize_leaf_blight", "dis_maize_stalk_rot"],
    "crop_soybean": ["dis_root_rot", "dis_virus_complex"],
    "crop_potato": ["dis_potato_late_blight", "dis_potato_early_blight"],
    "crop_peanut": ["dis_root_rot"],
    "crop_rapeseed": ["dis_gray_mold", "dis_virus_complex"],
    "crop_cotton": ["dis_root_rot"],
    "crop_tomato": [
        "dis_tomato_late_blight",
        "dis_tomato_early_blight",
        "dis_tomato_leaf_mold",
        "dis_gray_mold",
        "dis_virus_complex",
    ],
    "crop_cucumber": ["dis_cucumber_downy", "dis_cucumber_powdery", "dis_cucumber_wilt", "dis_gray_mold"],
    "crop_pepper": ["dis_pepper_phytophthora", "dis_pepper_anthracnose", "dis_virus_complex"],
    "crop_eggplant": ["dis_gray_mold", "dis_virus_complex"],
    "crop_cabbage": ["dis_gray_mold"],
    "crop_broccoli": ["dis_gray_mold"],
    "crop_carrot": ["dis_root_rot"],
    "crop_spinach": ["dis_gray_mold"],
    "crop_lettuce": ["dis_gray_mold"],
    "crop_celery": ["dis_gray_mold"],
    "crop_garlic": ["dis_root_rot"],
    "crop_onion": ["dis_gray_mold"],
    "crop_apple": ["dis_apple_canker", "dis_apple_ring_rot", "dis_apple_powdery"],
    "crop_pear": ["dis_apple_ring_rot", "dis_gray_mold"],
    "crop_peach": ["dis_root_rot", "dis_gray_mold"],
    "crop_grape": ["dis_grape_downy", "dis_grape_powdery", "dis_gray_mold"],
    "crop_citrus": ["dis_citrus_canker", "dis_citrus_sooty"],
    "crop_strawberry": ["dis_strawberry_powdery", "dis_strawberry_gray", "dis_gray_mold", "dis_root_rot"],
    "crop_watermelon": ["dis_gray_mold", "dis_virus_complex"],
    "crop_melon": ["dis_cucumber_powdery", "dis_gray_mold", "dis_virus_complex"],
}

for crop, dis_list in CROP_DIS.items():
    for d in dis_list:
        if d in E:
            e(crop, d, "易感", REF_TECH, 0.88)

# 作物-虫害
CROP_PEST = {
    "crop_rice": ["pest_rice_planthopper", "pest_rice_stem_borer"],
    "crop_wheat": ["pest_aphid", "pest_armyworm"],
    "crop_maize": ["pest_corn_borer", "pest_armyworm", "pest_aphid"],
    "crop_soybean": ["pest_aphid", "pest_spider_mite"],
    "crop_cotton": ["pest_cotton_bollworm", "pest_aphid", "pest_whitefly"],
    "crop_tomato": ["pest_whitefly", "pest_aphid", "pest_thrips", "pest_spider_mite"],
    "crop_cucumber": ["pest_whitefly", "pest_aphid", "pest_thrips", "pest_nematode"],
    "crop_pepper": ["pest_thrips", "pest_aphid", "pest_whitefly"],
    "crop_cabbage": ["pest_dbm", "pest_cabbage_worm", "pest_aphid"],
    "crop_broccoli": ["pest_dbm", "pest_cabbage_worm"],
    "crop_apple": ["pest_aphid", "pest_spider_mite"],
    "crop_citrus": ["pest_aphid", "pest_thrips", "pest_fruit_fly"],
    "crop_grape": ["pest_aphid", "pest_leafminer"],
    "crop_strawberry": ["pest_aphid", "pest_thrips", "pest_nematode"],
}
for crop, plist in CROP_PEST.items():
    for p in plist:
        e(crop, p, "常受害虫", REF_PEST, 0.82)

# 环境-病害诱发
e("env_high_humidity", "dis_gray_mold", "利于发生", REF_ENV, 0.9)
e("env_continuous_rain", "dis_rice_blast", "利于发生", REF_ENV, 0.85)
e("env_continuous_rain", "dis_potato_late_blight", "利于发生", REF_ENV, 0.9)
e("env_continuous_rain", "dis_tomato_late_blight", "利于发生", REF_ENV, 0.88)
e("env_dense_planting", "dis_rice_sheath_blight", "利于发生", REF_TECH, 0.8)
e("env_poor_ventilation", "dis_gray_mold", "利于发生", REF_ENV, 0.87)
e("env_heavy_dew", "dis_cucumber_downy", "利于发生", REF_ENV, 0.86)
e("env_heavy_dew", "dis_grape_downy", "利于发生", REF_ENV, 0.86)

# 逆境链
e("stress_soil_moisture_low", "risk_water_stress", "可能导致", "农业知识库-墒情与生理", None)
e("stress_drought", "risk_water_stress", "可能导致", REF_ENV, 0.92)
e("risk_water_stress", "sym_wilt", "常表现", REF_IRR, 0.85)
e("stress_waterlogging", "dis_root_rot", "易诱发", REF_IRR, 0.78)
e("stress_salinity", "stress_drought", "叠加", REF_ENV, 0.7)
e("prac_greenhouse_manage", "stress_low_temp", "可减轻", REF_TECH, 0.72)
e("prac_greenhouse_manage", "stress_high_temp", "可减轻", REF_TECH, 0.7)

# 土壤-管理
e("soil_low_organic", "def_n", "易伴随", REF_SOIL, 0.75)
e("soil_acid", "fert_lime", "常用改良", REF_SOIL, 0.88)
e("soil_alkaline", "def_zn", "易诱发", REF_SOIL, 0.72)
e("soil_sandy", "stress_soil_moisture_low", "更易出现", REF_SOIL, 0.8)
e("soil_clay", "stress_waterlogging", "更易出现", REF_SOIL, 0.78)
e("soil_loam", "crop_tomato", "适宜栽培", REF_SOIL, 0.75)
e("soil_loam", "crop_cucumber", "适宜栽培", REF_SOIL, 0.75)

# 缺素-症状
e("def_n", "sym_yellow_leaf", "典型表现", REF_FERT, 0.9)
e("def_k", "sym_yellow_leaf", "叶缘焦枯相关", REF_FERT, 0.8)
e("def_ca", "sym_spot", "果实生理病害相关", REF_FERT, 0.75)

# 肥料-养分
e("fert_urea", "nut_n", "主要提供", REF_FERT, 0.99)
e("fert_dap", "nut_n", "提供", REF_FERT, 0.95)
e("fert_dap", "nut_p", "主要提供", REF_FERT, 0.99)
e("fert_kcl", "nut_k", "主要提供", REF_FERT, 0.99)
e("fert_compound", "nut_n", "提供", REF_FERT, None)
e("fert_compound", "nut_p", "提供", REF_FERT, None)
e("fert_compound", "nut_k", "提供", REF_FERT, None)
e("fert_borate", "nut_b", "补充", REF_FERT, 0.98)
e("fert_zn_sulfate", "nut_zn", "补充", REF_FERT, 0.98)

# 作物-大量需求（简化：禾本科重氮钾，果菜重钾钙）
for c in ["crop_rice", "crop_wheat", "crop_maize"]:
    e(c, "nut_n", "需求较高", REF_FERT, 0.85)
    e(c, "nut_k", "需求较高", REF_FERT, 0.82)
for c in ["crop_tomato", "crop_cucumber", "crop_pepper", "crop_potato"]:
    e(c, "nut_k", "需求较高", REF_FERT, 0.88)
e("crop_apple", "nut_k", "果实品质相关需求", REF_FERT, 0.8)
e("crop_grape", "nut_k", "果实品质相关需求", REF_FERT, 0.8)
e("crop_strawberry", "nut_ca", "果实硬度相关", REF_FERT, 0.76)

# 缺素与作物敏感
for c in ["crop_grape", "crop_apple", "crop_strawberry"]:
    e(c, "def_b", "花期易缺", REF_FERT, 0.72)
e("crop_citrus", "def_zn", "易出现黄化", REF_FERT, 0.78)
e("crop_maize", "def_zn", "易出现", REF_FERT, 0.72)

# 农艺-缓解
e("prac_irrigation", "stress_drought", "缓解", REF_IRR, 0.95)
e("prac_drip", "stress_soil_moisture_low", "缓解", REF_IRR, 0.9)
e("prac_drainage", "stress_waterlogging", "缓解", REF_IRR, 0.92)
e("prac_ventilation", "env_high_humidity", "降低", REF_TECH, 0.88)
e("prac_ventilation", "dis_gray_mold", "辅助防控", REF_TECH, 0.7)
e("prac_prune", "env_dense_planting", "改善", REF_TECH, 0.75)
e("prac_formula_fert", "def_n", "矫正", REF_FERT, 0.85)
e("prac_formula_fert", "def_p", "矫正", REF_FERT, 0.85)
e("prac_formula_fert", "def_k", "矫正", REF_FERT, 0.85)
e("prac_compost", "soil_low_organic", "改良", REF_SOIL, 0.9)
e("prac_resistant_var", "dis_rice_blast", "降低风险", REF_TECH, 0.8)
e("prac_resistant_var", "dis_wheat_scab", "降低风险", REF_TECH, 0.75)
e("prac_seed_treat", "dis_root_rot", "降低苗期风险", REF_TECH, 0.72)
e("prac_ipm", "pest_aphid", "防控", REF_PEST, 0.8)
e("prac_yellow_sticky", "pest_whitefly", "监测防控", REF_PEST, 0.65)
e("prac_bio_control", "pest_dbm", "辅助防控", REF_PEST, 0.6)
e("prac_greenhouse_manage", "dis_gray_mold", "辅助防控", REF_TECH, 0.7)
e("prac_soil_test", "prac_formula_fert", "支撑", REF_SOIL, 0.95)

# 生育期-风险
e("stage_flowering", "dis_gray_mold", "敏感期", REF_TECH, 0.78)
e("stage_fruit", "def_ca", "敏感期", REF_FERT, 0.74)
e("stage_grain_fill", "def_k", "敏感期", REF_FERT, 0.8)

# 症状-病害提示（非一一对应，知识关联）
e("dis_gray_mold", "sym_mold", "可见", REF_TECH, 0.9)
e("dis_cucumber_downy", "sym_spot", "可见", REF_TECH, 0.88)
e("dis_virus_complex", "sym_yellow_leaf", "可见", REF_TECH, 0.65)

# 贮藏
e("crop_apple", "env_high_humidity", "贮藏期忌", REF_STORE, 0.7)
e("crop_strawberry", "env_high_humidity", "采后易腐", REF_STORE, 0.85)

# 药肥工具（抽象）
e("prac_ipm", "chem_fungicide", "可包含", REF_TECH, None)
e("prac_ipm", "chem_insecticide", "可包含", REF_TECH, None)

# 更多作物-缺素
for c in ["crop_rice", "crop_wheat", "crop_maize"]:
    e(c, "def_n", "追肥常见目标", REF_FERT, 0.7)
for c in ["crop_tomato", "crop_potato"]:
    e(c, "def_k", "膨大期关注", REF_FERT, 0.82)

# 扩展：更多虫害-作物
for c in ["crop_eggplant", "crop_pepper", "crop_tomato"]:
    e("pest_whitefly", c, "可危害", REF_PEST, 0.8)
e("pest_armyworm", "crop_maize", "主要危害", REF_PEST, 0.92)
e("pest_armyworm", "crop_wheat", "可危害", REF_PEST, 0.75)
e("pest_corn_borer", "crop_maize", "主要危害", REF_PEST, 0.95)

# 扩展：轮作（简化链）
e("prac_rotation", "dis_root_rot", "减轻连作障碍", REF_SOIL, 0.72)
e("prac_rotation", "pest_nematode", "减轻", REF_SOIL, 0.68)

# ---------- 批量扩展（农艺常识，用于丰富图谱） ----------
# 缺素 → 常用肥料
for defid, fertid, conf in [
    ("def_n", "fert_urea", 0.88),
    ("def_p", "fert_dap", 0.85),
    ("def_k", "fert_kcl", 0.88),
    ("def_b", "fert_borate", 0.9),
    ("def_zn", "fert_zn_sulfate", 0.9),
    ("def_ca", "fert_lime", 0.82),
]:
    e(defid, fertid, "常用矫正", REF_FERT, conf)

# 禾谷类灌浆期、苗期
for c in ["crop_rice", "crop_wheat", "crop_maize", "crop_soybean", "crop_peanut", "crop_rapeseed"]:
    e(c, "stage_seedling", "生育阶段", REF_TECH, None)
    e(c, "stage_grain_fill", "生育阶段", REF_TECH, None)

e("crop_cotton", "stage_seedling", "生育阶段", REF_TECH, None)
e("crop_cotton", "stage_fruit", "蕾铃期", REF_TECH, None)

# 根茎与薯类
for c in ["crop_potato", "crop_carrot", "crop_garlic", "crop_onion"]:
    e(c, "stage_seedling", "生育阶段", REF_TECH, None)
    e(c, "stage_fruit", "产品形成期", REF_TECH, None)

# 果菜与瓜类
for c in [
    "crop_tomato",
    "crop_cucumber",
    "crop_pepper",
    "crop_eggplant",
    "crop_cabbage",
    "crop_broccoli",
    "crop_spinach",
    "crop_lettuce",
    "crop_celery",
]:
    e(c, "stage_flowering", "生育阶段", REF_TECH, None)
    e(c, "stage_fruit", "产品形成期", REF_TECH, None)

for c in ["crop_strawberry", "crop_melon", "crop_watermelon"]:
    e(c, "stage_flowering", "生育阶段", REF_TECH, None)
    e(c, "stage_fruit", "产品形成期", REF_TECH, None)

# 果树
for c in ["crop_apple", "crop_pear", "crop_peach", "crop_grape", "crop_citrus"]:
    e(c, "stage_flowering", "生育阶段", REF_TECH, None)
    e(c, "stage_fruit", "产品形成期", REF_TECH, None)

# 壤土普遍适种（弱关联）
for cid, _, _ in CROPS:
    e("soil_loam", cid, "普遍适宜", REF_SOIL, 0.62)

# 砂土/黏土管理要点
e("soil_sandy", "prac_drip", "推荐结合", REF_IRR, 0.78)
e("soil_sandy", "prac_mulch", "保墒常用", REF_IRR, 0.74)
e("soil_clay", "prac_drainage", "需重视", REF_IRR, 0.85)
e("soil_clay", "stress_waterlogging", "雨后风险", REF_SOIL, 0.72)
e("soil_acid", "def_ca", "易诱发", REF_SOIL, 0.7)
e("soil_acid", "def_mg", "易诱发", REF_SOIL, 0.65)
e("soil_low_organic", "prac_compost", "推荐改良", REF_SOIL, 0.92)

# 中微量元素与作物
for c in ["crop_citrus", "crop_apple", "crop_grape"]:
    e(c, "nut_mg", "叶部营养相关", REF_FERT, 0.68)
    e(c, "nut_fe", "易出现缺铁黄化", REF_FERT, 0.66)
for c in ["crop_wheat", "crop_maize"]:
    e(c, "nut_mn", "生长相关", REF_FERT, 0.6)
for c in ["crop_onion", "crop_garlic"]:
    e(c, "nut_s", "风味物质相关", REF_FERT, 0.58)

# 未单独列虫害表的作物：蚜虫可发生
PEST_LISTED = set(CROP_PEST.keys())
for cid, _, _ in CROPS:
    if cid not in PEST_LISTED:
        e(cid, "pest_aphid", "可发生", REF_PEST, 0.58)

# 蓟马花器危害（补充谓词）
for c in ["crop_pepper", "crop_tomato", "crop_cucumber", "crop_strawberry", "crop_melon"]:
    e("pest_thrips", c, "花器危害", REF_PEST, 0.76)

# 线虫设施蔬菜
for c in ["crop_cucumber", "crop_melon", "crop_watermelon", "crop_tomato", "crop_pepper"]:
    e("pest_nematode", c, "连作地风险", REF_SOIL, 0.71)

# 环境-病害补充
e("env_high_humidity", "dis_strawberry_gray", "利于发生", REF_ENV, 0.88)
e("env_high_humidity", "dis_apple_powdery", "利于发生", REF_ENV, 0.76)
e("env_continuous_rain", "dis_wheat_scab", "利于发生", REF_ENV, 0.87)
e("env_continuous_rain", "dis_grape_downy", "利于发生", REF_ENV, 0.84)
e("stress_low_temp", "dis_wheat_powdery", "冷凉季相关", REF_ENV, 0.66)
e("env_poor_ventilation", "dis_tomato_leaf_mold", "利于发生", REF_TECH, 0.8)

# 灌溉方式-逆境
e("prac_sprinkler", "env_high_humidity", "可能加重叶面湿度", REF_IRR, 0.55)
e("prac_drip", "env_high_humidity", "利于降湿", REF_IRR, 0.62)

# 防控对象
e("chem_fungicide", "dis_gray_mold", "化学防控", REF_TECH, None)
e("chem_fungicide", "dis_cucumber_downy", "化学防控", REF_TECH, None)
e("chem_insecticide", "pest_rice_planthopper", "化学防控", REF_PEST, None)
e("chem_insecticide", "pest_cotton_bollworm", "化学防控", REF_PEST, None)

# 叶面肥关联
e("nut_b", "def_b", "矫正目标", REF_FERT, None)
e("nut_zn", "def_zn", "矫正目标", REF_FERT, None)

# 覆膜与深松配套
for c in ["crop_cotton", "crop_peanut", "crop_potato", "crop_tomato", "crop_strawberry", "crop_watermelon"]:
    e(c, "prac_mulch", "常配套使用", REF_TECH, 0.68)
for c in ["crop_maize", "crop_soybean", "crop_cotton", "crop_potato"]:
    e(c, "prac_deep_till", "增产配套", REF_SOIL, 0.63)

e("crop_rice", "prac_irrigation", "水分管理核心", REF_IRR, 0.94)
e("crop_wheat", "prac_formula_fert", "高产常用", REF_FERT, 0.8)

for d in ["dis_rice_blast", "dis_wheat_scab", "dis_gray_mold", "dis_cucumber_downy"]:
    e("prac_ipm", d, "综合治理", REF_TECH, 0.58)

for c in ["crop_cabbage", "crop_broccoli", "crop_spinach", "crop_lettuce"]:
    e(c, "nut_n", "叶菜需氮高", REF_FERT, 0.86)
for c in ["crop_carrot", "crop_potato"]:
    e(c, "nut_k", "品质形成相关", REF_FERT, 0.8)

for c in ["crop_pear", "crop_peach", "crop_apple"]:
    e(c, "prac_prune", "树体管理", REF_TECH, 0.88)

e("def_mg", "fert_compound", "常用矫正", REF_FERT, 0.75)
e("fert_compound", "nut_mg", "可提供", REF_FERT, None)
e("fert_compound", "nut_s", "可提供", REF_FERT, None)

# 去重（头、尾、谓词）
seen: set[tuple[str, str, str]] = set()
out_edges: list[tuple[str, str, str, str, float | None]] = []
for tup in edges:
    h, t, p, ref, conf = tup
    if h not in E or t not in E:
        continue
    key = (h, t, p)
    if key in seen:
        continue
    seen.add(key)
    out_edges.append(tup)

rows = []
for h, t, pred, ref, conf in out_edges:
    hn, hk = E[h]
    tn, tk = E[t]
    row = {
        "headId": h,
        "headName": hn,
        "headKind": hk,
        "tailId": t,
        "tailName": tn,
        "tailKind": tk,
        "predicate": pred,
        "sourceRef": ref,
    }
    if conf is not None:
        row["confidence"] = conf
    rows.append(row)

out = Path(__file__).resolve().parent.parent / "src/main/resources/knowledge-graph/agri-graph-seed.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(rows, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print(f"wrote {len(rows)} triples to {out}")

# 实体业务说明（写入 Neo4j description / aliases，供详情 API 与前端抽屉）
DESC_TEMPLATES: dict[str, str] = {
    "crop": "{name}是农业生产中的栽培对象，与病虫害、水肥管理、土壤环境及农艺措施存在多类关联，可用于风险研判与农技推荐。",
    "disease": "{name}为田间常见病害类型，发生与作物品种、环境条件及栽培管理相关，宜结合绿色防控与监测预警。",
    "pest": "{name}为农业害虫或有害生物类别，需关注发生期与作物敏感期，采取综合防治。",
    "stress": "{name}描述作物生长中的逆境因子，常与产量与品质波动相关，可通过灌溉、施肥与设施调控缓解。",
    "practice": "{name}为可操作的农艺或管理措施，用于改善墒情、养分、群体结构或病虫害防控效果。",
    "environment": "{name}表示影响作物生长与病害发生的环境情境，在图谱中常作为诱发或加重因子的上下文节点。",
    "soil": "{name}反映土壤类型或肥力相关特征，与施肥、灌溉及根系环境密切相关。",
    "nutrient": "{name}为植物必需营养元素维度，与缺素诊断与肥料运筹直接相关。",
    "deficiency": "{name}表示养分亏缺状态，可通过土壤与植株诊断结合测土配方施肥矫正。",
    "fertilizer": "{name}为常见肥料或调理剂类型，用于补充氮磷钾及中微量元素。",
    "pesticide": "{name}为农药防控手段的抽象分类，实际使用须遵守登记与安全间隔期规定。",
    "growth_stage": "{name}表示作物生育进程阶段，不同阶段对水肥药及逆境敏感性不同。",
    "symptom": "{name}为田间可观察的症状表型，可用于辅助推断缺素或病害方向（须结合检测）。",
    "concept": "{name}为知识网络中的抽象概念节点，用于连接胁迫、风险与农艺逻辑。",
    "other": "{name}为农业知识图谱中的实体节点。",
}

entities_obj: dict[str, dict] = {}
for eid, (name, kind) in E.items():
    tmpl = DESC_TEMPLATES.get(kind, DESC_TEMPLATES["other"])
    entities_obj[eid] = {
        "description": tmpl.format(name=name),
        "aliases": [],
    }

ent_out = out.parent / "agri-graph-entities.json"
ent_out.write_text(json.dumps(entities_obj, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"wrote {len(entities_obj)} entity profiles to {ent_out}")
