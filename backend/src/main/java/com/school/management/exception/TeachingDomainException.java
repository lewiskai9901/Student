package com.school.management.exception;

/**
 * 教务领域异常
 * 用于教学任务、考试、成绩、排课等业务规则违反
 */
public class TeachingDomainException extends BusinessException {

    public TeachingDomainException(String message) {
        super(ErrorCode.OPERATION_NOT_ALLOWED, message);
    }

    public TeachingDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TeachingDomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static TeachingDomainException examBatchNotFound(Long id) {
        return new TeachingDomainException(ErrorCode.EXAM_BATCH_NOT_FOUND, "考试批次不存在: " + id);
    }

    public static TeachingDomainException examBatchAlreadyPublished(Long id) {
        return new TeachingDomainException(ErrorCode.EXAM_BATCH_ALREADY_PUBLISHED, "考试批次已发布: " + id);
    }

    public static TeachingDomainException examBatchNotReady(String reason) {
        return new TeachingDomainException(ErrorCode.EXAM_BATCH_NOT_READY, reason);
    }

    public static TeachingDomainException gradeBatchNotFound(Long id) {
        return new TeachingDomainException(ErrorCode.GRADE_BATCH_NOT_FOUND, "成绩批次不存在: " + id);
    }

    public static TeachingDomainException gradeBatchAlreadyPublished(Long id) {
        return new TeachingDomainException(ErrorCode.GRADE_BATCH_ALREADY_PUBLISHED, "成绩批次已发布: " + id);
    }

    public static TeachingDomainException gradeBatchNotReady(String reason) {
        return new TeachingDomainException(ErrorCode.GRADE_BATCH_NOT_READY, reason);
    }

    public static TeachingDomainException taskNotFound(Long id) {
        return new TeachingDomainException(ErrorCode.TEACHING_TASK_NOT_FOUND, "教学任务不存在: " + id);
    }

    public static TeachingDomainException scheduleConflict(String detail) {
        return new TeachingDomainException(ErrorCode.SCHEDULE_CONFLICT, detail);
    }

    public static TeachingDomainException offeringNotFound(Long id) {
        return new TeachingDomainException(ErrorCode.OFFERING_NOT_FOUND, "开课计划不存在: " + id);
    }

    public static TeachingDomainException offeringNotDraft(Integer status) {
        return new TeachingDomainException(ErrorCode.OFFERING_NOT_DRAFT,
                "只能确认草稿状态的开课计划, 当前状态: " + status);
    }

    public static TeachingDomainException constraintNotFound(Long id) {
        return new TeachingDomainException(ErrorCode.SCHEDULING_CONSTRAINT_NOT_FOUND, "排课约束不存在: " + id);
    }
}
