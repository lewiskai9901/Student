package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.AppealApprovalConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 申诉审批流配置Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface AppealApprovalConfigMapper extends BaseMapper<AppealApprovalConfig> {

    /**
     * 查询申诉配置的审批流
     *
     * @param appealConfigId 申诉配置ID
     * @return 审批流配置列表
     */
    List<AppealApprovalConfig> selectByAppealConfigId(@Param("appealConfigId") Long appealConfigId);

    /**
     * 查询申诉配置的必须审批步骤
     *
     * @param appealConfigId 申诉配置ID
     * @return 审批流配置列表
     */
    List<AppealApprovalConfig> selectRequiredSteps(@Param("appealConfigId") Long appealConfigId);

    /**
     * 查询申诉配置的可选审批步骤
     *
     * @param appealConfigId 申诉配置ID
     * @return 审批流配置列表
     */
    List<AppealApprovalConfig> selectOptionalSteps(@Param("appealConfigId") Long appealConfigId);

    /**
     * 批量插入审批流配置
     *
     * @param configs 审批流配置列表
     * @return 插入行数
     */
    int batchInsert(@Param("configs") List<AppealApprovalConfig> configs);

    /**
     * 删除申诉配置的所有审批流
     *
     * @param appealConfigId 申诉配置ID
     * @return 删除行数
     */
    int deleteByAppealConfigId(@Param("appealConfigId") Long appealConfigId);
}
