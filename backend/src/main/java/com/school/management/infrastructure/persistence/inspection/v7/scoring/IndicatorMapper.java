package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface IndicatorMapper extends BaseMapper<IndicatorPO> {

    @Select("SELECT * FROM insp_indicators WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sort_order")
    List<IndicatorPO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM insp_indicators WHERE parent_indicator_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<IndicatorPO> findByParentIndicatorId(@Param("parentId") Long parentId);

    @Update("UPDATE insp_indicators SET deleted = 1 WHERE project_id = #{projectId} AND deleted = 0")
    void softDeleteByProjectId(@Param("projectId") Long projectId);
}
