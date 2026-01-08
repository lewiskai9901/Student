package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CheckPlanRatingAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评级审核日志Mapper
 */
@Mapper
public interface CheckPlanRatingAuditLogMapper extends BaseMapper<CheckPlanRatingAuditLog> {
}
