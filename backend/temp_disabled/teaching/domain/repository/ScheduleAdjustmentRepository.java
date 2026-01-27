package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.entity.ScheduleAdjustment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 调课申请仓储接口
 */
public interface ScheduleAdjustmentRepository {

    /**
     * 保存调课申请
     */
    ScheduleAdjustment save(ScheduleAdjustment adjustment);

    /**
     * 根据ID查询
     */
    Optional<ScheduleAdjustment> findById(Long id);

    /**
     * 根据调课单号查询
     */
    Optional<ScheduleAdjustment> findByAdjustmentCode(String adjustmentCode);

    /**
     * 查询学期的所有调课申请
     */
    List<ScheduleAdjustment> findBySemesterId(Long semesterId);

    /**
     * 查询申请人的调课申请
     */
    List<ScheduleAdjustment> findByApplicantId(Long applicantId);

    /**
     * 查询待审批的调课申请
     */
    List<ScheduleAdjustment> findPending(Long semesterId);

    /**
     * 按日期查询调课申请
     */
    List<ScheduleAdjustment> findByOriginalDate(LocalDate date);

    /**
     * 按审批状态查询
     */
    List<ScheduleAdjustment> findByApprovalStatus(Long semesterId, Integer approvalStatus);

    /**
     * 分页查询
     */
    List<ScheduleAdjustment> findPage(int page, int size, Long semesterId, Long applicantId, Integer approvalStatus);

    /**
     * 统计总数
     */
    long count(Long semesterId, Long applicantId, Integer approvalStatus);

    /**
     * 删除调课申请
     */
    void deleteById(Long id);

    /**
     * 检查调课单号是否存在
     */
    boolean existsByAdjustmentCode(String adjustmentCode);

    /**
     * 生成调课单号
     */
    String generateAdjustmentCode();
}
