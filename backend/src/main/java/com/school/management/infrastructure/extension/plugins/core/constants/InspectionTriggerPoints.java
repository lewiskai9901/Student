package com.school.management.infrastructure.extension.plugins.core.constants;

/**
 * 检查平台触发点常量 — 检查是通用能力,放 core.
 * 检查模板/评分体系由各行业插件贡献,但事件管道通用.
 */
public final class InspectionTriggerPoints {
    private InspectionTriggerPoints() {}

    /** 单项检查结果(打分/判定完成) */
    public static final String INSP_ITEM_RESULT    = "INSP_ITEM_RESULT";

    /** 等级结果(按分数段评级) */
    public static final String INSP_GRADE_RESULT   = "INSP_GRADE_RESULT";

    /** 单条提交完成 */
    public static final String INSP_RECORD_COMPLETE = "INSP_RECORD_COMPLETE";
}
