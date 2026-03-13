package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通用空间类型 Mapper
 */
@Mapper
public interface UniversalSpaceTypeMapper extends BaseMapper<UniversalSpaceTypePO> {

    /**
     * 根据类型编码查询
     */
    @Select("SELECT * FROM space_types WHERE type_code = #{typeCode} AND deleted = 0")
    UniversalSpaceTypePO findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 查询所有启用的类型
     */
    @Select("SELECT * FROM space_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UniversalSpaceTypePO> findAllEnabled();

    /**
     * 查询所有根类型
     */
    @Select("SELECT * FROM space_types WHERE is_root_type = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UniversalSpaceTypePO> findAllRootTypes();

    /**
     * 检查类型编码是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM space_types WHERE type_code = #{typeCode} AND deleted = 0")
    boolean existsByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 检查类型是否被使用
     */
    @Select("SELECT COUNT(*) > 0 FROM spaces WHERE type_code = #{typeCode} AND deleted = 0")
    boolean isTypeInUse(@Param("typeCode") String typeCode);

    /**
     * 批量查询
     */
    @Select("<script>" +
            "SELECT * FROM space_types WHERE type_code IN " +
            "<foreach collection='typeCodes' item='code' open='(' separator=',' close=')'>" +
            "#{code}" +
            "</foreach>" +
            " AND deleted = 0 ORDER BY sort_order" +
            "</script>")
    List<UniversalSpaceTypePO> findByTypeCodes(@Param("typeCodes") List<String> typeCodes);
}
