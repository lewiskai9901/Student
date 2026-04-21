package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for roles (DDD infrastructure).
 * Maps to the existing 'roles' table.
 */
@Mapper
public interface DddRoleMapper extends BaseMapper<RolePO> {

    @Select("SELECT * FROM roles WHERE role_code = #{code} AND deleted = 0")
    RolePO findByRoleCode(@Param("code") String code);

    @Select("SELECT * FROM roles WHERE deleted = 0 AND plugin_enabled = 1 ORDER BY sort_order")
    List<RolePO> findAll();

    /**
     * 管理员视角: 可选是否包含 plugin_enabled=0 的行.
     * @param includeDisabled true 时返回被禁插件贡献的角色 (灰显), false 时与 findAll 等价
     */
    @Select("<script>" +
            "SELECT * FROM roles WHERE deleted = 0" +
            "<if test='!includeDisabled'> AND plugin_enabled = 1</if>" +
            " ORDER BY sort_order" +
            "</script>")
    List<RolePO> findAllForAdmin(@Param("includeDisabled") boolean includeDisabled);

    @Select("SELECT * FROM roles WHERE status = 1 AND plugin_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<RolePO> findAllEnabled();

    /**
     * 管理员视角的已启用 (status=1) 列表: 可选包含 plugin_enabled=0 的行.
     */
    @Select("<script>" +
            "SELECT * FROM roles WHERE status = 1 AND deleted = 0" +
            "<if test='!includeDisabled'> AND plugin_enabled = 1</if>" +
            " ORDER BY sort_order" +
            "</script>")
    List<RolePO> findAllEnabledForAdmin(@Param("includeDisabled") boolean includeDisabled);

    @Select("SELECT COUNT(*) > 0 FROM roles WHERE role_code = #{code} AND deleted = 0")
    boolean existsByRoleCode(@Param("code") String code);

    @Select("<script>" +
            "SELECT * FROM roles WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = 0" +
            "</script>")
    List<RolePO> findByIds(@Param("ids") List<Long> ids);
}
