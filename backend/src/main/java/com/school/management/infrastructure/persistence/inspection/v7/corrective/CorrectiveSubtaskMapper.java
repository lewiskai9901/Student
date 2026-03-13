package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CorrectiveSubtaskMapper extends BaseMapper<CorrectiveSubtaskPO> {

    @Select("SELECT * FROM insp_corrective_subtasks WHERE case_id = #{caseId} AND deleted = 0 ORDER BY sort_order, created_at")
    List<CorrectiveSubtaskPO> findByCaseId(@Param("caseId") Long caseId);

    @Select("SELECT COUNT(*) FROM insp_corrective_subtasks WHERE case_id = #{caseId} AND status = #{status} AND deleted = 0")
    int countByCaseIdAndStatus(@Param("caseId") Long caseId, @Param("status") String status);
}
