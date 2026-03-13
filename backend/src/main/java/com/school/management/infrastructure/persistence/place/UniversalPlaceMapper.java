package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通用空间 Mapper
 */
@Mapper
public interface UniversalSpaceMapper extends BaseMapper<UniversalSpacePO> {

    /**
     * 根据空间编码查询
     */
    @Select("SELECT * FROM spaces WHERE space_code = #{spaceCode} AND deleted = 0")
    UniversalSpacePO findBySpaceCode(@Param("spaceCode") String spaceCode);

    /**
     * 查询所有根空间
     */
    @Select("SELECT * FROM spaces WHERE parent_id IS NULL AND deleted = 0 ORDER BY space_name")
    List<UniversalSpacePO> findAllRoots();

    /**
     * 查询子空间
     */
    @Select("SELECT * FROM spaces WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY space_name")
    List<UniversalSpacePO> findChildren(@Param("parentId") Long parentId);

    /**
     * 根据路径前缀查询所有后代
     */
    @Select("SELECT * FROM spaces WHERE path LIKE CONCAT(#{pathPrefix}, '%') AND deleted = 0 ORDER BY level, space_name")
    List<UniversalSpacePO> findByPathPrefix(@Param("pathPrefix") String pathPrefix);

    /**
     * 根据类型查询
     */
    @Select("SELECT * FROM spaces WHERE type_code = #{typeCode} AND deleted = 0 ORDER BY space_name")
    List<UniversalSpacePO> findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 根据组织单元查询
     */
    @Select("SELECT * FROM spaces WHERE org_unit_id = #{orgUnitId} AND deleted = 0 ORDER BY space_name")
    List<UniversalSpacePO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据负责人查询
     */
    @Select("SELECT * FROM spaces WHERE responsible_user_id = #{userId} AND deleted = 0 ORDER BY space_name")
    List<UniversalSpacePO> findByResponsibleUserId(@Param("userId") Long userId);

    /**
     * 检查空间编码是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM spaces WHERE space_code = #{spaceCode} AND deleted = 0")
    boolean existsBySpaceCode(@Param("spaceCode") String spaceCode);

    /**
     * 统计子空间数量
     */
    @Select("SELECT COUNT(*) FROM spaces WHERE parent_id = #{parentId} AND deleted = 0")
    int countChildren(@Param("parentId") Long parentId);

    /**
     * 统计后代空间数量
     */
    @Select("SELECT COUNT(*) FROM spaces WHERE path LIKE CONCAT(#{pathPrefix}, '%') AND id != #{ancestorId} AND deleted = 0")
    int countDescendants(@Param("ancestorId") Long ancestorId, @Param("pathPrefix") String pathPrefix);

    /**
     * 带关联信息查询
     */
    @Select("SELECT s.*, " +
            "st.type_name, " +
            "p.space_name AS parent_name, " +
            "o.name AS org_unit_name, " +
            "u.name AS responsible_user_name " +
            "FROM spaces s " +
            "LEFT JOIN space_types st ON s.type_code = st.type_code AND st.deleted = 0 " +
            "LEFT JOIN spaces p ON s.parent_id = p.id AND p.deleted = 0 " +
            "LEFT JOIN org_unit o ON s.org_unit_id = o.id AND o.deleted = 0 " +
            "LEFT JOIN user u ON s.responsible_user_id = u.id AND u.deleted = 0 " +
            "WHERE s.id = #{id} AND s.deleted = 0")
    UniversalSpacePO findByIdWithRelations(@Param("id") Long id);
}
