package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 组织类型 Mapper
 */
@Mapper
public interface OrgUnitTypeMapper extends BaseMapper<OrgUnitTypePO> {

    @Select("SELECT * FROM org_unit_types WHERE type_code = #{typeCode} AND deleted = 0")
    OrgUnitTypePO findByTypeCode(@Param("typeCode") String typeCode);

    @Select("SELECT * FROM org_unit_types WHERE parent_type_code = #{parentTypeCode} AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitTypePO> findByParentTypeCode(@Param("parentTypeCode") String parentTypeCode);

    @Select("SELECT * FROM org_unit_types WHERE (parent_type_code IS NULL OR parent_type_code = '') AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitTypePO> findTopLevelTypes();

    @Select("SELECT * FROM org_unit_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitTypePO> findAllEnabled();

    @Select("SELECT * FROM org_unit_types WHERE category = #{category} AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitTypePO> findByCategory(@Param("category") String category);

    @Select("SELECT * FROM org_unit_types WHERE JSON_EXTRACT(features, CONCAT('$.', #{featureKey})) = true AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<OrgUnitTypePO> findByFeature(@Param("featureKey") String featureKey);

    @Select("SELECT COUNT(*) > 0 FROM org_unit_types WHERE type_code = #{typeCode} AND deleted = 0")
    boolean existsByTypeCode(@Param("typeCode") String typeCode);

    @Select("SELECT COUNT(*) FROM org_units WHERE unit_type = #{typeCode} AND deleted = 0")
    int countOrgUnitsByTypeCode(@Param("typeCode") String typeCode);

    @Update("UPDATE org_unit_types SET deleted = id, updated_at = NOW() WHERE id = #{id} AND deleted = 0")
    int softDeleteById(@Param("id") Long id);
}
