package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for permissions (DDD infrastructure).
 * Maps to the existing 'permissions' table.
 */
@Mapper
public interface DddPermissionMapper extends BaseMapper<PermissionPO> {

    @Select("SELECT * FROM permissions WHERE permission_code = #{code} AND deleted = 0")
    PermissionPO findByPermissionCode(@Param("code") String code);

    @Select("SELECT * FROM permissions WHERE resource_type = #{resourceType} AND deleted = 0 ORDER BY sort_order")
    List<PermissionPO> findByResourceType(@Param("resourceType") Integer resourceType);

    @Select("SELECT * FROM permissions WHERE status = 1 AND deleted = 0 ORDER BY sort_order")
    List<PermissionPO> findAllEnabled();

    @Select("SELECT * FROM permissions WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<PermissionPO> findByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM permissions WHERE (parent_id IS NULL OR parent_id = 0) AND deleted = 0 ORDER BY sort_order")
    List<PermissionPO> findRoots();

    @Select("SELECT * FROM permissions WHERE permission_code LIKE CONCAT(#{resource}, ':%') AND deleted = 0 ORDER BY sort_order")
    List<PermissionPO> findByResource(@Param("resource") String resource);

    @Select("SELECT COUNT(*) > 0 FROM permissions WHERE permission_code = #{code} AND deleted = 0")
    boolean existsByPermissionCode(@Param("code") String code);

    @Select("<script>" +
            "SELECT * FROM permissions WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = 0" +
            "</script>")
    List<PermissionPO> findByIds(@Param("ids") List<Long> ids);
}
