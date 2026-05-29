package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.scoring.NormalizeBy;

/**
 * 归一化分母解析器 — 按 (检查目标类型, 归一化维度) 算出 population 分母.
 * 通用核心契约: 引擎不知道"班级/学生/教室", 只问本 resolver 要分母.
 * 分母来源限定三个基础模块通用字段 (成员数/场所容量/子组织数). 行业插件可实现本接口覆盖.
 */
public interface NormalizationBasisResolver {
    /** @return 归一化分母; 无法解析或 USER 单体返回 1 */
    int resolveDenominator(TargetType targetType, Long targetId, NormalizeBy normalizeBy);
}
