package com.school.management.domain.inspection.model.v7.corrective;

/**
 * 根因分析方法枚举
 */
public enum RcaMethod {
    NONE,        // 无根因分析
    FIVE_WHYS,   // 5-Why 分析法
    FISHBONE,    // 鱼骨图 (石川图)
    FREE_TEXT    // 自由文本描述
}
