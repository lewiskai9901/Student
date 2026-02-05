package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 组织类型Mapper
 */
@Mapper
public interface OrgUnitTypeMapper extends BaseMapper<OrgUnitTypePO> {

    /**
     * 根据类型编码查询
     */
    @Select("SELECT * FROM org_unit_types WHERE type_code = #{typeCode} AND deleted = 0")
    OrgUnitTypePO findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 根据父类型编码查询子类型
     */
    @Select("SELECT * FROM org_unit_types WHERE parent_type_code = #{parentTypeCode} AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitTypePO> findByParentTypeCode(@Param("parentTypeCode") String parentTypeCode);

    /**
     * 查询顶级类型
     */
    @Select("SELECT * FROM org_unit_types WHERE (parent_type_code IS NULL OR parent_type_code = '') AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitTypePO> findTopLevelTypes();

    /**
     * 查询所有启用的类型
     */
    @Select("SELECT * FROM org_unit_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY level_order, sort_order")
    List<OrgUnitTypePO> findAllEnabled();

    /**
     * 查询教学单位类型
     */
    @Select("SELECT * FROM org_unit_types WHERE is_academic = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY level_order, sort_order")
    List<OrgUnitTypePO> findAcademicTypes();

    /**
     * 查询职能部门类型
     */
    @Select("SELECT * FROM org_unit_types WHERE is_academic = 0 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitTypePO> findFunctionalTypes();

    /**
     * 查询可检查的类型
     */
    @Select("SELECT * FROM org_unit_types WHERE can_be_inspected = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY level_order, sort_order")
    List<OrgUnitTypePO> findInspectableTypes();

    /**
     * 检查类型编码是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM org_unit_types WHERE type_code = #{typeCode} AND deleted = 0")
    boolean existsByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 统计使用该类型的组织单元数量
     */
    @Select("SELECT COUNT(*) FROM org_units WHERE unit_type = #{typeCode} AND deleted = 0")
    int countOrgUnitsByTypeCode(@Param("typeCode") String typeCode);
}
