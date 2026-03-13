package com.school.management.domain.inspection.model.v7.template;

/**
 * V7 模板字段类型枚举 (22种)
 */
public enum ItemType {
    // 文本
    TEXT,
    TEXTAREA,
    RICH_TEXT,

    // 数值
    NUMBER,
    SLIDER,
    CALCULATED,

    // 选择
    SELECT,
    MULTI_SELECT,
    CHECKBOX,
    RADIO,

    // 日期时间
    DATE,
    TIME,
    DATETIME,

    // 媒体/文件
    PHOTO,
    VIDEO,
    SIGNATURE,
    FILE_UPLOAD,

    // 特殊
    GPS,
    BARCODE,

    // 评分专用
    RATING,
    PASS_FAIL,
    CHECKLIST;
}
