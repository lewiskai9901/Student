package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

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
    @DataPermission(module = "place", orgUnitField = "effective_org_unit_id")
    @Select("SELECT * FROM places WHERE parent_id IS NULL AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findAllRoots();

    /**
     * 查询子空间
     */
    @DataPermission(module = "place", orgUnitField = "effective_org_unit_id")
    @Select("SELECT * FROM places WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findChildren(@Param("parentId") Long parentId);

    /**
     * 根据路径前缀查询所有后代
     */
    @DataPermission(module = "place", orgUnitField = "effective_org_unit_id")
    @Select("SELECT * FROM places WHERE path LIKE CONCAT(#{pathPrefix}, '%') AND deleted = 0 ORDER BY level, place_name")
    List<UniversalPlacePO> findByPathPrefix(@Param("pathPrefix") String pathPrefix);

    /**
     * 根据类型查询
     */
    @DataPermission(module = "place", orgUnitField = "effective_org_unit_id")
    @Select("SELECT * FROM places WHERE type_code = #{typeCode} AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 根据组织单元查询
     */
    @DataPermission(module = "place", orgUnitField = "effective_org_unit_id")
    @Select("SELECT * FROM places WHERE effective_org_unit_id = #{orgUnitId} AND deleted = 0 ORDER BY place_name")
    List<UniversalPlacePO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据负责人查询 — 经 responsible_for 关系覆盖点 (语义等价旧列: 仅显式责任人, 不含继承)
     */
    @DataPermission(module = "place", orgUnitField = "effective_org_unit_id")
    @Select("SELECT p.* FROM places p " +
            "JOIN access_relations ar ON ar.resource_type = 'place' AND ar.resource_id = p.id " +
            "  AND ar.relation = 'responsible_for' AND ar.subject_type = 'user' " +
            "  AND ar.subject_id = #{userId} AND ar.deleted = 0 " +
            "WHERE p.deleted = 0 ORDER BY p.place_name")
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
     * 原子递增占用数（数据库级并发安全，仅在未超容量时生效）
     */
    @Update("UPDATE places SET current_occupancy = COALESCE(current_occupancy, 0) + 1 " +
            "WHERE id = #{id} AND deleted = 0 " +
            "AND (capacity IS NULL OR capacity = 0 OR COALESCE(current_occupancy, 0) < capacity)")
    int atomicIncrementOccupancy(@Param("id") Long id);

    /**
     * 原子递减占用数（数据库级并发安全，仅在占用数 > 0 时生效）
     */
    @Update("UPDATE places SET current_occupancy = COALESCE(current_occupancy, 0) - 1 " +
            "WHERE id = #{id} AND deleted = 0 " +
            "AND COALESCE(current_occupancy, 0) > 0")
    int atomicDecrementOccupancy(@Param("id") Long id);

    /**
     * 查找 currentOccupancy 与实际占用数不匹配的场所
     */
    @Select("SELECT p.id, COALESCE(p.current_occupancy, 0) AS storedCount, " +
            "COALESCE(oc.actual_count, 0) AS actualCount " +
            "FROM places p " +
            "LEFT JOIN (" +
            "  SELECT place_id, COUNT(*) AS actual_count " +
            "  FROM place_occupants " +
            "  WHERE status = 1 AND deleted = 0 " +
            "  GROUP BY place_id" +
            ") oc ON p.id = oc.place_id " +
            "WHERE p.deleted = 0 " +
            "AND COALESCE(p.current_occupancy, 0) != COALESCE(oc.actual_count, 0)")
    List<Map<String, Object>> findOccupancyMismatches();

    /**
     * 修正场所占用数
     */
    @Update("UPDATE places SET current_occupancy = #{actualCount} WHERE id = #{placeId} AND deleted = 0")
    int fixOccupancy(@Param("placeId") Long placeId, @Param("actualCount") int actualCount);

}
