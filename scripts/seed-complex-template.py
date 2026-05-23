#!/usr/bin/env python3
"""
2026-05-24: 通过 API 批量构造一个复杂检查模板, 用于演示重构后系统的能力.

模板: 「教学楼综合巡查标准 (五级综合评估)」
- 5 个根分区, 权重 30 / 25 / 20 / 15 / 10 (合计 100)
- 公共区域分区再拆 2 个子分区 (走廊楼梯 60% / 卫生间 40%)
- 约 24 个 items, 覆盖 RADIO/NUMBER/PHOTO/SIGNATURE/GPS/DATE/TEXTAREA 等类型
- 2 个 ResponseSet 共享: 5 级评估 + 3 级状态
"""
import json
import sys
import urllib.request
import urllib.error
from typing import Any, Optional

BASE = "http://localhost:8080/api"

# Windows 系统代理常把 localhost 也代理掉 → 502. 绕过代理直连.
_OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def http(method: str, path: str, token: str, body: Optional[dict] = None,
         params: Optional[dict] = None) -> dict:
    url = BASE + path
    if params:
        qs = "&".join(f"{k}={v}" for k, v in params.items())
        url = f"{url}?{qs}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", f"Bearer {token}")
    req.add_header("Content-Type", "application/json")
    try:
        with _OPENER.open(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        msg = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {path} -> {e.code}\n{msg}") from None


def login() -> str:
    resp = http("POST", "/auth/login", token="",
                body={"username": "admin", "password": "admin123"})
    return resp["data"]["accessToken"]


def main():
    token = login()
    print(f"[login] ok, token len={len(token)}")

    # ============================================================
    # 1. ResponseSet A: 5 级评估
    # ============================================================
    rs5 = http("POST", "/inspection/response-sets", token, body={
        "setCode": "RS_5_LEVEL_EVAL",
        "setName": "五级评估 (优/良/中/差/不合格)",
        "isGlobal": True,
    })["data"]
    rs5_id = rs5["id"]
    print(f"[rs] 5-level set id={rs5_id}")

    for opt in [
        {"optionValue": "A", "optionLabel": "优 (90-100)", "optionColor": "#16a34a", "score": 100, "isFlagged": False, "sortOrder": 1},
        {"optionValue": "B", "optionLabel": "良 (80-89)",  "optionColor": "#2563eb", "score": 85,  "isFlagged": False, "sortOrder": 2},
        {"optionValue": "C", "optionLabel": "中 (70-79)",  "optionColor": "#ca8a04", "score": 75,  "isFlagged": False, "sortOrder": 3},
        {"optionValue": "D", "optionLabel": "差 (60-69)",  "optionColor": "#ea580c", "score": 65,  "isFlagged": True,  "sortOrder": 4},
        {"optionValue": "E", "optionLabel": "不合格 (<60)", "optionColor": "#dc2626", "score": 30, "isFlagged": True,  "sortOrder": 5},
    ]:
        http("POST", f"/inspection/response-sets/{rs5_id}/options", token, body=opt)
    print(f"  + 5 options")

    # ============================================================
    # 2. ResponseSet B: 3 级状态 (设施类用)
    # ============================================================
    rs3 = http("POST", "/inspection/response-sets", token, body={
        "setCode": "RS_3_STATUS",
        "setName": "三级状态 (正常/告警/故障)",
        "isGlobal": True,
    })["data"]
    rs3_id = rs3["id"]
    print(f"[rs] 3-status set id={rs3_id}")
    for opt in [
        {"optionValue": "OK",    "optionLabel": "正常", "optionColor": "#16a34a", "score": 100, "isFlagged": False, "sortOrder": 1},
        {"optionValue": "WARN",  "optionLabel": "告警", "optionColor": "#ca8a04", "score": 60,  "isFlagged": True,  "sortOrder": 2},
        {"optionValue": "ERROR", "optionLabel": "故障", "optionColor": "#dc2626", "score": 0,   "isFlagged": True,  "sortOrder": 3},
    ]:
        http("POST", f"/inspection/response-sets/{rs3_id}/options", token, body=opt)
    print(f"  + 3 options")

    # ============================================================
    # 3. 根分区 (模板)
    # ============================================================
    root = http("POST", "/inspection/templates", token, body={
        "name": "教学楼综合巡查标准 (五级综合评估)",
        "description": "覆盖教学楼日常巡查 5 个维度: 教室卫生 / 公共区域 / 设施设备 / 安全消防 / 现场签收. 含 ~24 项检查指标, 多类型字段 + 5 级评估融合.",
        "tags": "教学楼,综合,日常巡查,演示模板",
        "targetType": "PLACE",
    })["data"]
    root_id = root["id"]
    print(f"[root] template id={root_id}")

    # ============================================================
    # 4. 5 个二级分区 + 子分区 + items
    # ============================================================
    def add_section(code: str, name: str, parent_id: Optional[int],
                    weight: int, order: int) -> int:
        body = {
            "sectionCode": code,
            "sectionName": name,
            "parentSectionId": parent_id,
            "weight": weight,
            "sortOrder": order,
            "rootSectionId": root_id,
        }
        return http("POST", "/inspection/sections", token, body=body)["data"]["id"]

    def add_item(section_id: int, code: str, name: str, item_type: str,
                 weight: int = 10, order: int = 1, required: bool = True,
                 scored: bool = True, evidence: bool = False,
                 response_set_id: Optional[int] = None,
                 description: str = "") -> int:
        body = {
            "itemCode": code,
            "itemName": name,
            "description": description,
            "itemType": item_type,
            "itemWeight": weight,
            "sortOrder": order,
            "isRequired": required,
            "isScored": scored,
            "requireEvidence": evidence,
            "responseSetId": response_set_id,
        }
        return http("POST", f"/inspection/sections/{section_id}/items", token, body=body)["data"]["id"]

    # --- Section 1: 教室卫生 (weight 30) ---
    s1 = add_section("SEC_CLASS_HYGIENE", "教室卫生", root_id, weight=30, order=1)
    print(f"[s1] 教室卫生 id={s1}")
    add_item(s1, "CH_01", "桌椅整齐度",   "RADIO",  weight=15, order=1, response_set_id=rs5_id, description="评估桌椅摆放、损耗、整洁程度")
    add_item(s1, "CH_02", "地面清洁",     "RADIO",  weight=15, order=2, response_set_id=rs5_id)
    add_item(s1, "CH_03", "黑板白板",     "RADIO",  weight=10, order=3, response_set_id=rs5_id)
    add_item(s1, "CH_04", "垃圾分类",     "RADIO",  weight=15, order=4, response_set_id=rs5_id, description="是否按 4 类 (可回收/有害/厨余/其他) 正确分类")
    add_item(s1, "CH_05", "通风换气",     "RADIO",  weight=10, order=5, response_set_id=rs5_id)
    add_item(s1, "CH_06", "现场照片",     "PHOTO",  weight=10, order=6, required=True, scored=False, evidence=True, description="拍照至少 2 张, 覆盖前后视角")
    add_item(s1, "CH_07", "卫生问题数",   "NUMBER", weight=15, order=7, required=False, scored=True, description="发现的卫生问题数 (0=全部合格)")
    add_item(s1, "CH_08", "其他备注",     "TEXTAREA", weight=10, order=8, required=False, scored=False)

    # --- Section 2: 公共区域 (weight 25) ---
    s2 = add_section("SEC_COMMON", "公共区域", root_id, weight=25, order=2)
    print(f"[s2] 公共区域 id={s2}")
    # Section 2a: 走廊楼梯 (60%)
    s2a = add_section("SEC_CORRIDOR", "走廊楼梯", s2, weight=60, order=1)
    add_item(s2a, "CR_01", "地面清洁",   "RADIO", weight=30, order=1, response_set_id=rs5_id)
    add_item(s2a, "CR_02", "扶手干净",   "RADIO", weight=25, order=2, response_set_id=rs5_id)
    add_item(s2a, "CR_03", "灯光照明",   "RADIO", weight=25, order=3, response_set_id=rs3_id, description="正常 = 所有灯具工作, 告警 = 个别故障, 故障 = 区域性熄灭")
    add_item(s2a, "CR_04", "通道畅通",   "RADIO", weight=20, order=4, response_set_id=rs5_id)
    # Section 2b: 卫生间 (40%)
    s2b = add_section("SEC_RESTROOM", "卫生间", s2, weight=40, order=2)
    add_item(s2b, "RT_01", "异味",       "RADIO", weight=30, order=1, response_set_id=rs5_id)
    add_item(s2b, "RT_02", "设施完好",   "RADIO", weight=30, order=2, response_set_id=rs3_id)
    add_item(s2b, "RT_03", "卫生用品",   "RADIO", weight=20, order=3, response_set_id=rs5_id, description="纸巾/洗手液/烘干机是否完备")
    add_item(s2b, "RT_04", "现场照片",   "PHOTO", weight=20, order=4, scored=False, required=True, evidence=True)

    # --- Section 3: 设施设备 (weight 20) ---
    s3 = add_section("SEC_EQUIP", "设施设备", root_id, weight=20, order=3)
    print(f"[s3] 设施设备 id={s3}")
    add_item(s3, "EQ_01", "投影仪状态",   "RADIO",  weight=25, order=1, response_set_id=rs3_id)
    add_item(s3, "EQ_02", "电源插座",     "RADIO",  weight=20, order=2, response_set_id=rs3_id)
    add_item(s3, "EQ_03", "饮水机",       "RADIO",  weight=15, order=3, response_set_id=rs3_id)
    add_item(s3, "EQ_04", "故障数量",     "NUMBER", weight=20, order=4, description="检查中发现的故障设备总数")
    add_item(s3, "EQ_05", "设备照片",     "PHOTO",  weight=10, order=5, scored=False, evidence=True)
    add_item(s3, "EQ_06", "上次保养日期", "DATE",   weight=10, order=6, required=False, scored=False)

    # --- Section 4: 安全消防 (weight 15) ---
    s4 = add_section("SEC_SAFETY", "安全消防", root_id, weight=15, order=4)
    print(f"[s4] 安全消防 id={s4}")
    add_item(s4, "SF_01", "灭火器有效期",  "DATE",     weight=25, order=1, description="检查至少 1 个灭火器有效期, 若已过期记 0 分")
    add_item(s4, "SF_02", "应急通道",      "RADIO",    weight=25, order=2, response_set_id=rs5_id)
    add_item(s4, "SF_03", "消防栓状态",    "RADIO",    weight=20, order=3, response_set_id=rs3_id)
    add_item(s4, "SF_04", "监控覆盖",      "RADIO",    weight=15, order=4, response_set_id=rs3_id)
    add_item(s4, "SF_05", "安全隐患备注",  "TEXTAREA", weight=15, order=5, required=False, scored=False, description="发现的隐患描述 + 拟整改建议")

    # --- Section 5: 现场签收 (weight 10) ---
    s5 = add_section("SEC_SIGN", "现场签收", root_id, weight=10, order=5)
    print(f"[s5] 现场签收 id={s5}")
    add_item(s5, "SG_01", "检查员签字",      "SIGNATURE", weight=40, order=1, required=True, scored=False)
    add_item(s5, "SG_02", "在场负责人姓名",  "TEXT",      weight=20, order=2, required=True, scored=False)
    add_item(s5, "SG_03", "GPS 坐标",        "GPS",       weight=20, order=3, required=True, scored=False, description="自动采集检查地点 GPS")
    add_item(s5, "SG_04", "总体评价",        "TEXTAREA",  weight=20, order=4, required=False, scored=False)

    print()
    print(f"[done] 复杂模板已创建: root id={root_id}")
    print(f"  - 5 个根分区 (权重 30+25+20+15+10 = 100)")
    print(f"  - 2 个子分区 (公共区域→走廊楼梯+卫生间)")
    print(f"  - 24 个检查项 (RADIO/NUMBER/PHOTO/SIGNATURE/GPS/DATE/TEXTAREA)")
    print(f"  - 2 个选项集 (5 级评估 + 3 级状态) 复用绑定")
    print()
    print(f"前端入口: http://localhost:3000/inspection/v7/templates/{root_id}/edit")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
