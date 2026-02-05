package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 组织类型 Mapper 接口
 */
@Mapper
public interface OrgTypeMapper extends BaseMapper<OrgTypePO> {

    /**
     * 根据类型编码查询
     */
    @Select("SELECT * FROM org_types WHERE type_code = #{typeCode} AND deleted = 0")
    OrgTypePO findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 查询指定父类型下的子类型
     */
    @Select("SELECT * FROM org_types WHERE parent_type_code = #{parentTypeCode} AND deleted = 0 ORDER BY sort_order, id")
    List<OrgTypePO> findByParentTypeCode(@Param("parentTypeCode") String parentTypeCode);

    /**
     * 查询所有顶级类型
     */
    @Select("SELECT * FROM org_types WHERE (parent_type_code IS NULL OR parent_type_code = '') AND deleted = 0 ORDER BY sort_order, id")
    List<OrgTypePO> findTopLevelTypes();

    /**
     * 查询所有启用的类型
     */
    @Select("SELECT * FROM org_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY level_order, sort_order, id")
    List<OrgTypePO> findAllEnabled();

    /**
     * 查询指定层级的类型
     */
    @Select("SELECT * FROM org_types WHERE level_order = #{levelOrder} AND deleted = 0 ORDER BY sort_order, id")
    List<OrgTypePO> findByLevelOrder(@Param("levelOrder") Integer levelOrder);

    /**
     * 检查类型编码是否存在
     */
    @Select("SELECT COUNT(*) FROM org_types WHERE type_code = #{typeCode} AND deleted = 0")
    int existsByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 检查类型是否被组织单元使用
     */
    @Select("SELECT COUNT(*) FROM org_units WHERE type_code = #{typeCode} AND deleted = 0")
    int countOrgUnitsByTypeCode(@Param("typeCode") String typeCode);
}
