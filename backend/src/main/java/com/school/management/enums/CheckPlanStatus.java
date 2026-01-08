package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 检查计划状态枚举
 *
 * @author system
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CheckPlanStatus {

    DRAFT(0, "草稿"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    ARCHIVED(3, "已归档");

    private final Integer code;
    private final String desc;

    public static CheckPlanStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CheckPlanStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        CheckPlanStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知";
    }

    /**
     * 检查是否可以从当前状态转换到目标状态
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 是否允许转换
     */
    public static boolean canTransition(Integer from, Integer to) {
        if (from == null || to == null) {
            return false;
        }
        // 草稿 -> 进行中
        if (from == DRAFT.code && to == IN_PROGRESS.code) {
            return true;
        }
        // 进行中 -> 已完成
        if (from == IN_PROGRESS.code && to == COMPLETED.code) {
            return true;
        }
        // 已完成 -> 已归档
        if (from == COMPLETED.code && to == ARCHIVED.code) {
            return true;
        }
        return false;
    }
}
