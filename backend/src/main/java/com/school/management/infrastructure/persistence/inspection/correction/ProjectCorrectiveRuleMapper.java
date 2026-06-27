package com.school.management.infrastructure.persistence.inspection.correction;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "inspection_corrective_rule")
public interface ProjectCorrectiveRuleMapper extends BaseMapper<ProjectCorrectiveRulePO> {
}
