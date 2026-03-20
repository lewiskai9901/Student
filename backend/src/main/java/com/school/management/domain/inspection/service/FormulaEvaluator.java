package com.school.management.domain.inspection.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 公式求值器 — 领域接口
 * 基础设施层提供 GraalVM JS 实现
 */
public interface FormulaEvaluator {

    /**
     * 执行公式并返回计算结果
     *
     * @param formula   JS 表达式, 如 "score * quantity * (baseline / population)"
     * @param variables 变量映射, 如 {"score": -2, "quantity": 3, "population": 50, "baseline": 40}
     * @return 计算结果
     */
    BigDecimal evaluate(String formula, Map<String, Object> variables);
}
