package com.school.management.infrastructure.persistence.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for schedule policies.
 */
@Mapper
public interface SchedulePolicyMapper extends BaseMapper<SchedulePolicyPO> {

    @Select("SELECT * FROM schedule_policies WHERE policy_code = #{policyCode} AND deleted = 0")
    SchedulePolicyPO findByPolicyCode(@Param("policyCode") String policyCode);

    @Select("SELECT * FROM schedule_policies WHERE is_enabled = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<SchedulePolicyPO> findEnabled();

    @Select("SELECT * FROM schedule_policies WHERE template_id = #{templateId} AND deleted = 0 ORDER BY created_at DESC")
    List<SchedulePolicyPO> findByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM schedule_policies WHERE deleted = 0 ORDER BY created_at DESC")
    List<SchedulePolicyPO> findAll();
}
