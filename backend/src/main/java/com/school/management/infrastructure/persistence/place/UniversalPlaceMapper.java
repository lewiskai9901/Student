package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通用空间 Mapper
 */
@Mapper
public interface UniversalPlaceMapper extends BaseMapper<UniversalPlacePO> {

    /**
     * 根据空间编码查询
     */
    @Select("SELECT * FROM places WHERE place_code = #{placeCode} AND deleted = 0")
    UniversalPlacePO findByPlaceCode(@Param("placeCode") String placeCode);

    /**
     * 查询所有根空间
     */
    @Select("SELECT * FROM places WHERE parent_id IS NULL AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findAllRoots();

    /**
     * 查询子空间
     */
    @Select("SELECT * FROM places WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findChildren(@Param("parentId") Long parentId);

    /**
     * 根据路径前缀查询所有后代
     */
    @Select("SELECT * FROM places WHERE path LIKE CONCAT(#{pathPrefix}, '%') AND deleted = 0 ORDER BY level, place_name")
    List<UniversalPlacePO> findByPathPrefix(@Param("pathPrefix") String pathPrefix);

    /**
     * 根据类型查询
     */
    @Select("SELECT * FROM places WHERE type_code = #{typeCode} AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 根据组织单元查询
     */
    @Select("SELECT * FROM places WHERE org_unit_id = #{orgUnitId} AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据负责人查询
     */
    @Select("SELECT * FROM places WHERE responsible_user_id = #{userId} AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findByResponsibleUserId(@Param("userId") Long userId);

    /**
     * 检查空间编码是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM places WHERE place_code = #{placeCode} AND deleted = 0")
    boolean existsByPlaceCode(@Param("placeCode") String placeCode);

    /**
     * 统计子空间数量
     */
    @Select("SELECT COUNT(*) FROM places WHERE parent_id = #{parentId} AND deleted = 0")
    int countChildren(@Param("parentId") Long parentId);

    /**
     * 统计后代空间数量
     */
    @Select("SELECT COUNT(*) FROM places WHERE path LIKE CONCAT(#{pathPrefix}, '%') AND id != #{ancestorId} AND deleted = 0")
    int countDescendants(@Param("ancestorId") Long ancestorId, @Param("pathPrefix") String pathPrefix);

    /**
     * 统计同父节点下相同编号的数量
     */
    @Select("SELECT COUNT(*) FROM places WHERE parent_id = #{parentId} AND place_code = #{placeCode} AND deleted = 0")
    int countByParentIdAndPlaceCode(@Param("parentId") Long parentId, @Param("placeCode") String placeCode);

    /**
     * 统计根节点中相同编号的数量
     */
    @Select("SELECT COUNT(*) FROM places WHERE parent_id IS NULL AND place_code = #{placeCode} AND deleted = 0")
    int countRootByPlaceCode(@Param("placeCode") String placeCode);

    /**
     * 带关联信息查询
     */
    @Select("SELECT s.*, " +
            "st.type_name, " +
            "p.place_name AS parent_name, " +
            "o.unit_name AS org_unit_name, " +
            "u.real_name AS responsible_user_name " +
            "FROM places s " +
            "LEFT JOIN place_types st ON s.type_code = st.type_code AND st.deleted = 0 " +
            "LEFT JOIN places p ON s.parent_id = p.id AND p.deleted = 0 " +
            "LEFT JOIN org_units o ON s.org_unit_id = o.id AND o.deleted = 0 " +
            "LEFT JOIN users u ON s.responsible_user_id = u.id AND u.deleted = 0 " +
            "WHERE s.id = #{id} AND s.deleted = 0")
    UniversalPlacePO findByIdWithRelations(@Param("id") Long id);
}
