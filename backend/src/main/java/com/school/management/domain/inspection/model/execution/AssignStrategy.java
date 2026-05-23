package com.school.management.domain.inspection.model.execution;

/**
 * 调度组检查员指派策略 (V20260524_3 引入, 干掉旧 inspectorIds 空字符串 falsy magic).
 *
 * <p>语义:
 * <ul>
 *   <li>{@link #SPECIFIC} - 限定到 insp_plan_inspectors 关系表里列出的检查员;
 *       必须至少 1 个 inspector, 否则违反业务不变量.</li>
 *   <li>{@link #OPEN_TO_ALL} - 项目全员可领取; 此时 insp_plan_inspectors 必须为空 (否则语义冲突).</li>
 * </ul>
 *
 * <p>修复了旧版"inspectorIds 空数组 = 全员可领取"的隐式语义 — 现在管理员清空成员
 * 列表如果没显式切到 OPEN_TO_ALL, 会抛 InvalidAssignStrategyException 提醒.
 */
public enum AssignStrategy {
    SPECIFIC,
    OPEN_TO_ALL
}
