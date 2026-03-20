package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通用场所类型 Mapper（统一类型系统 Phase 3）
 */
@Mapper
public interface UniversalPlaceTypeMapper extends BaseMapper<UniversalPlaceTypePO> {

    @Select("SELECT * FROM place_types WHERE type_code = #{typeCode} AND deleted = 0")
    UniversalPlaceTypePO findByTypeCode(@Param("typeCode") String typeCode);

    @Select("SELECT * FROM place_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UniversalPlaceTypePO> findAllEnabled();

    @Select("SELECT * FROM place_types WHERE is_root_type = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UniversalPlaceTypePO> findAllRootTypes();

    @Select("SELECT * FROM place_types WHERE parent_type_code = #{parentTypeCode} AND deleted = 0 ORDER BY sort_order")
    List<UniversalPlaceTypePO> findByParentTypeCode(@Param("parentTypeCode") String parentTypeCode);

    @Select("SELECT * FROM place_types WHERE category = #{category} AND deleted = 0 ORDER BY sort_order")
    List<UniversalPlaceTypePO> findByCategory(@Param("category") String category);

    @Select("SELECT COUNT(*) > 0 FROM place_types WHERE type_code = #{typeCode} AND deleted = 0")
    boolean existsByTypeCode(@Param("typeCode") String typeCode);

    @Select("SELECT COUNT(*) > 0 FROM places WHERE type_code = #{typeCode} AND deleted = 0")
    boolean isTypeInUse(@Param("typeCode") String typeCode);

    @Select("<script>" +
            "SELECT * FROM place_types WHERE type_code IN " +
            "<foreach collection='typeCodes' item='code' open='(' separator=',' close=')'>" +
            "#{code}" +
            "</foreach>" +
            " AND deleted = 0 ORDER BY sort_order" +
            "</script>")
    List<UniversalPlaceTypePO> findByTypeCodes(@Param("typeCodes") List<String> typeCodes);

    /** 基础分类: category IS NULL AND is_system = 1 */
    @Select("SELECT * FROM place_types WHERE category IS NULL AND is_system = 1 AND deleted = 0 ORDER BY sort_order")
    List<UniversalPlaceTypePO> findAllBaseCategories();

    /** 具体根类型: 有 category 且 isRootType=1 */
    @Select("SELECT * FROM place_types WHERE category IS NOT NULL AND is_root_type = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UniversalPlaceTypePO> findConcreteRootTypes();

    /** 所有具体类型 */
    @Select("SELECT * FROM place_types WHERE category IS NOT NULL AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UniversalPlaceTypePO> findAllConcreteTypes();
}
