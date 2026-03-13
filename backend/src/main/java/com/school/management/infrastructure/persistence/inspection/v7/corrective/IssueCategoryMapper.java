package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IssueCategoryMapper extends BaseMapper<IssueCategoryPO> {

    @Select("SELECT * FROM insp_issue_categories WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<IssueCategoryPO> findByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM insp_issue_categories WHERE parent_id IS NULL AND deleted = 0 ORDER BY sort_order")
    List<IssueCategoryPO> findRoots();

    @Select("SELECT * FROM insp_issue_categories WHERE deleted = 0 ORDER BY sort_order")
    List<IssueCategoryPO> findAllActive();
}
