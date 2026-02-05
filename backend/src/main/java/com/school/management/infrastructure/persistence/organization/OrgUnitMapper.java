package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 组织单元 Mapper
 * 映射到 org_units 表
 */
@Mapper
public interface OrgUnitMapper extends BaseMapper<OrgUnitPO> {

    /**
     * 根据树路径前缀查询所有后代节点
     */
    @Select("SELECT * FROM org_units WHERE tree_path LIKE CONCAT(#{treePath}, '%') AND deleted = 0 ORDER BY tree_level, sort_order")
    List<OrgUnitPO> selectDescendants(@Param("treePath") String treePath);

    /**
     * 统计某个父节点下的子节点数量
     */
    @Select("SELECT COUNT(*) FROM org_units WHERE parent_id = #{parentId} AND deleted = 0")
    long countByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有启用的组织
     */
    @Select("SELECT * FROM org_units WHERE status = 1 AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitPO> findAllEnabled();

    /**
     * 根据组织编码查询
     */
    @Select("SELECT * FROM org_units WHERE unit_code = #{code} AND deleted = 0")
    OrgUnitPO findByUnitCode(@Param("code") String code);

    /**
     * 查询根节点（无父节点）
     */
    @Select("SELECT * FROM org_units WHERE parent_id IS NULL AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitPO> findRoots();

    /**
     * 根据父节点ID查询子节点
     */
    @Select("SELECT * FROM org_units WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitPO> findByParentId(@Param("parentId") Long parentId);

    /**
     * 根据组织类别查询
     */
    @Select("SELECT * FROM org_units WHERE unit_category = #{unitCategory} AND status = 1 AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitPO> findByUnitCategory(@Param("unitCategory") String unitCategory);

    /**
     * 根据组织类型查询
     */
    @Select("SELECT * FROM org_units WHERE unit_type = #{unitType} AND status = 1 AND deleted = 0 ORDER BY tree_level, sort_order")
    List<OrgUnitPO> findByUnitType(@Param("unitType") String unitType);
}
