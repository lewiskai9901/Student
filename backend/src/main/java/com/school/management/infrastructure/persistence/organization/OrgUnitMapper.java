package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for OrgUnit persistence.
 * Maps to the existing 'departments' table.
 */
@Mapper
public interface OrgUnitMapper extends BaseMapper<OrgUnitPO> {

    /**
     * Finds all descendants by department path prefix.
     */
    @Select("SELECT * FROM departments WHERE dept_path LIKE CONCAT(#{deptPath}, '%') AND deleted = 0 ORDER BY dept_level, sort_order")
    List<OrgUnitPO> selectDescendants(@Param("deptPath") String deptPath);

    /**
     * Counts children of a parent.
     */
    @Select("SELECT COUNT(*) FROM departments WHERE parent_id = #{parentId} AND deleted = 0")
    long countByParentId(@Param("parentId") Long parentId);

    /**
     * Find all enabled departments.
     */
    @Select("SELECT * FROM departments WHERE status = 1 AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitPO> findAllEnabled();

    /**
     * Find by department code.
     */
    @Select("SELECT * FROM departments WHERE dept_code = #{code} AND deleted = 0")
    OrgUnitPO findByDeptCode(@Param("code") String code);

    /**
     * Find root departments (no parent).
     */
    @Select("SELECT * FROM departments WHERE parent_id IS NULL AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitPO> findRoots();

    /**
     * Find by parent ID.
     */
    @Select("SELECT * FROM departments WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitPO> findByParentId(@Param("parentId") Long parentId);

    /**
     * Find by unit category (functional, academic, administrative).
     */
    @Select("SELECT * FROM departments WHERE unit_category = #{unitCategory} AND status = 1 AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitPO> findByUnitCategory(@Param("unitCategory") String unitCategory);
}
