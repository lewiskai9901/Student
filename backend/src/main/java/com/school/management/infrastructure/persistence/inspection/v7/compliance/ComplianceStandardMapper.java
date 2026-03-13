package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ComplianceStandardMapper extends BaseMapper<ComplianceStandardPO> {

    @Select("SELECT * FROM insp_compliance_standards WHERE standard_code = #{code} AND deleted = 0 LIMIT 1")
    ComplianceStandardPO findByCode(@Param("code") String code);
}
