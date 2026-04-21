package com.school.management.infrastructure.persistence.shared;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 统一类型配置 Mapper (entity_type_configs)
 */
@Mapper
public interface EntityTypeConfigMapper extends BaseMapper<EntityTypeConfigPO> {

    @Select("SELECT * FROM entity_type_configs WHERE entity_type = #{entityType} AND type_code = #{typeCode} AND deleted = 0")
    EntityTypeConfigPO findByTypeCode(@Param("entityType") String entityType,
                                      @Param("typeCode") String typeCode);

    @Select("SELECT * FROM entity_type_configs WHERE entity_type = #{entityType} AND deleted = 0 ORDER BY sort_order, type_name")
    List<EntityTypeConfigPO> findAll(@Param("entityType") String entityType);

    @Select("SELECT * FROM entity_type_configs WHERE entity_type = #{entityType} AND is_enabled = 1 AND plugin_enabled = 1 AND deleted = 0 ORDER BY sort_order, type_name")
    List<EntityTypeConfigPO> findAllEnabled(@Param("entityType") String entityType);

    @Select("SELECT * FROM entity_type_configs WHERE entity_type = #{entityType} AND parent_type_code = #{parentTypeCode} AND deleted = 0 ORDER BY sort_order")
    List<EntityTypeConfigPO> findByParentTypeCode(@Param("entityType") String entityType,
                                                   @Param("parentTypeCode") String parentTypeCode);

    @Select("SELECT * FROM entity_type_configs WHERE entity_type = #{entityType} AND (parent_type_code IS NULL OR parent_type_code = '') AND deleted = 0 ORDER BY sort_order")
    List<EntityTypeConfigPO> findTopLevelTypes(@Param("entityType") String entityType);

    @Select("SELECT * FROM entity_type_configs WHERE entity_type = #{entityType} AND category = #{category} AND deleted = 0 ORDER BY sort_order")
    List<EntityTypeConfigPO> findByCategory(@Param("entityType") String entityType,
                                             @Param("category") String category);

    @Select("SELECT * FROM entity_type_configs WHERE entity_type = #{entityType} AND JSON_EXTRACT(features, CONCAT('$.', #{featureKey})) = true AND deleted = 0 ORDER BY sort_order")
    List<EntityTypeConfigPO> findByFeature(@Param("entityType") String entityType,
                                            @Param("featureKey") String featureKey);

    @Select("SELECT COUNT(*) > 0 FROM entity_type_configs WHERE entity_type = #{entityType} AND type_code = #{typeCode} AND deleted = 0")
    boolean existsByTypeCode(@Param("entityType") String entityType,
                             @Param("typeCode") String typeCode);
}
