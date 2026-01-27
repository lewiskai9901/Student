package com.school.management.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.task.TaskApprovalConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务审批配置Mapper
 *
 * @author Claude
 * @version 1.0
 * @since 2025-12-28
 */
@Mapper
public interface TaskApprovalConfigMapper extends BaseMapper<TaskApprovalConfig> {

    /**
     * 根据任务ID和组织单元ID查询审批配置
     *
     * @param taskId 任务ID
     * @param orgUnitId 组织单元ID
     * @return 审批配置列表（按审批级别升序）
     */
    List<TaskApprovalConfig> selectByTaskAndDept(@Param("taskId") Long taskId,
                                                  @Param("orgUnitId") Long orgUnitId);

    /**
     * 根据任务ID查询所有审批配置（按组织单元、级别分组）
     *
     * @param taskId 任务ID
     * @return 审批配置列表（按组织单元ID、审批级别升序）
     */
    List<TaskApprovalConfig> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 批量插入审批配置
     *
     * @param configs 审批配置列表
     * @return 插入记录数
     */
    int batchInsert(@Param("configs") List<TaskApprovalConfig> configs);
}
