package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ComplianceClauseMapper extends BaseMapper<ComplianceClausePO> {

    @Select("SELECT * FROM insp_compliance_clauses WHERE standard_id = #{standardId} AND deleted = 0 ORDER BY sort_order")
    List<ComplianceClausePO> findByStandardId(@Param("standardId") Long standardId);

    @Update("UPDATE insp_compliance_clauses SET deleted = 1 WHERE standard_id = #{standardId} AND deleted = 0")
    void softDeleteByStandardId(@Param("standardId") Long standardId);
}
