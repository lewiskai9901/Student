package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EscalationPolicyMapper extends BaseMapper<EscalationPolicyPO> {

    @Select("SELECT * FROM insp_escalation_policies WHERE profile_id = #{profileId} AND deleted = 0 ORDER BY id")
    List<EscalationPolicyPO> findByProfileId(Long profileId);
}
