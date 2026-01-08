package com.school.management.dto.quickentry;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快捷录入 - 检查重复响应DTO
 *
 * @author system
 * @since 1.0.7
 */
@Data
public class QuickEntryCheckDuplicateResponse {

    /**
     * 是否存在重复记录
     */
    private Boolean isDuplicate;

    /**
     * 重复记录的ID (如果存在)
     */
    private Long existingRecordId;

    /**
     * 重复记录的创建时间 (如果存在)
     */
    private LocalDateTime existingRecordTime;

    /**
     * 重复记录的操作人 (如果存在)
     */
    private String existingRecordOperator;

    /**
     * 提示信息
     */
    private String message;

    public static QuickEntryCheckDuplicateResponse noDuplicate() {
        QuickEntryCheckDuplicateResponse response = new QuickEntryCheckDuplicateResponse();
        response.setIsDuplicate(false);
        return response;
    }

    public static QuickEntryCheckDuplicateResponse duplicate(Long recordId, LocalDateTime createdAt, String operator) {
        QuickEntryCheckDuplicateResponse response = new QuickEntryCheckDuplicateResponse();
        response.setIsDuplicate(true);
        response.setExistingRecordId(recordId);
        response.setExistingRecordTime(createdAt);
        response.setExistingRecordOperator(operator);
        response.setMessage("该学生在此扣分项下已有记录");
        return response;
    }
}
