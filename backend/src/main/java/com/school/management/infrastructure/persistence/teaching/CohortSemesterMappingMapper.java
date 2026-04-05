package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CohortSemesterMappingMapper extends BaseMapper<CohortSemesterMappingPO> {

    @Select("SELECT * FROM cohort_semester_mapping WHERE cohort_id = #{cid} AND semester_id = #{sid}")
    CohortSemesterMappingPO findByCohortAndSemester(@Param("cid") Long cohortId, @Param("sid") Long semesterId);

    @Select("SELECT * FROM cohort_semester_mapping WHERE semester_id = #{sid} AND status = 1 ORDER BY cohort_id")
    List<CohortSemesterMappingPO> findBySemesterId(@Param("sid") Long semesterId);

    @Select("SELECT * FROM cohort_semester_mapping WHERE cohort_id = #{cid} AND status = 1 ORDER BY program_semester")
    List<CohortSemesterMappingPO> findByCohortId(@Param("cid") Long cohortId);
}
