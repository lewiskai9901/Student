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

    @Select("SELECT * FROM roles WHERE deleted = 0 ORDER BY sort_order")
    List<RolePO> findAll();

    @Select("SELECT * FROM roles WHERE status = 1 AND deleted = 0 ORDER BY sort_order")
    List<RolePO> findAllEnabled();

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
