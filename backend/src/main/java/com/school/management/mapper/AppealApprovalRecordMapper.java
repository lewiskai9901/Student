package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.AppealApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 申诉审批记录Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface AppealApprovalRecordMapper extends BaseMapper<AppealApprovalRecord> {

    /**
     * 查询申诉的所有审批记录
     *
     * @param appealId 申诉ID
     * @return 审批记录列表
     */
    List<AppealApprovalRecord> selectByAppealId(@Param("appealId") Long appealId);

    /**
     * 查询申诉的当前步骤审批记录
     *
     * @param appealId 申诉ID
     * @param stepOrder 步骤序号
     * @return 审批记录
     */
    AppealApprovalRecord selectByAppealAndStep(
            @Param("appealId") Long appealId,
            @Param("stepOrder") Integer stepOrder
    );

    /**
     * 查询审批人的待审批记录
     *
     * @param approverId 审批人ID
     * @return 待审批记录列表
     */
    List<AppealApprovalRecord> selectPendingByApprover(@Param("approverId") Long approverId);

    /**
     * 查询超时的审批记录
     *
     * @return 超时记录列表
     */
    List<AppealApprovalRecord> selectTimeoutRecords();

    /**
     * 批量插入审批记录
     *
     * @param records 审批记录列表
     * @return 插入行数
     */
    int batchInsert(@Param("records") List<AppealApprovalRecord> records);
}
