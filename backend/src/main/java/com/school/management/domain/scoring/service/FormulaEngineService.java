package com.school.management.domain.scoring.service;

import java.util.Map;

/**
 * 公式引擎服务接口
 */
public interface FormulaEngineService {

    /**
     * 执行公式并返回数值结果
     *
     * @param formula  公式字符串
     * @param context  上下文变量
     * @return 计算结果
     */
    Double evaluateNumeric(String formula, Map<String, Object> context);

    /**
     * 执行公式并返回布尔结果
     *
     * @param formula  公式字符串
     * @param context  上下文变量
     * @return 布尔结果
     */
    Boolean evaluateBoolean(String formula, Map<String, Object> context);

    /**
     * 执行公式并返回任意类型结果
     *
     * @param formula  公式字符串
     * @param context  上下文变量
     * @return 计算结果
     */
    Object evaluate(String formula, Map<String, Object> context);

    /**
     * 验证公式语法
     *
     * @param formula 公式字符串
     * @return 是否有效
     */
    boolean validate(String formula);

    /**
     * 获取公式执行错误信息
     *
     * @param formula 公式字符串
     * @param context 上下文变量
     * @return 错误信息，如果无错误则返回null
     */
    String getExecutionError(String formula, Map<String, Object> context);
}
