package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 检查会话状态枚举
 * 1=草稿, 2=待审核, 3=已发布
 *
 * @author system
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum InspectionSessionStatus {

    DRAFT(1, "草稿"),
    PENDING_REVIEW(2, "待审核"),
    PUBLISHED(3, "已发布");

    private final Integer code;
    private final String desc;

    public static InspectionSessionStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (InspectionSessionStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        InspectionSessionStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知";
    }

    /**
     * 检查是否为草稿状态（可编辑）
     */
    public static boolean isDraft(Integer code) {
        return DRAFT.code.equals(code);
    }

    /**
     * 检查是否为待审核状态
     */
    public static boolean isPendingReview(Integer code) {
        return PENDING_REVIEW.code.equals(code);
    }

    /**
     * 检查是否已发布
     */
    public static boolean isPublished(Integer code) {
        return PUBLISHED.code.equals(code);
    }
}
