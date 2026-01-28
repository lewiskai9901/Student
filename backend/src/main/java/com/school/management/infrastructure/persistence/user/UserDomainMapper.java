package com.school.management.infrastructure.persistence.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户领域 Mapper
 */
@Mapper
public interface UserDomainMapper extends BaseMapper<UserPO> {

    /**
     * 根据用户名查找用户
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    UserPO findByUsername(@Param("username") String username);

    /**
     * 根据手机号查找用户
     */
    @Select("SELECT * FROM users WHERE phone = #{phone} AND deleted = 0")
    UserPO findByPhone(@Param("phone") String phone);

    /**
     * 根据邮箱查找用户
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    UserPO findByEmail(@Param("email") String email);

    /**
     * 根据工号查找用户
     */
    @Select("SELECT * FROM users WHERE employee_no = #{employeeNo} AND deleted = 0")
    UserPO findByEmployeeNo(@Param("employeeNo") String employeeNo);

    /**
     * 根据微信OpenID查找用户
     */
    @Select("SELECT * FROM users WHERE wechat_openid = #{openid} AND deleted = 0")
    UserPO findByWechatOpenid(@Param("openid") String openid);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM users WHERE username = #{username} AND deleted = 0")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查用户名是否存在（排除指定ID）
     */
    @Select("SELECT COUNT(1) > 0 FROM users WHERE username = #{username} AND id != #{excludeId} AND deleted = 0")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("excludeId") Long excludeId);

    /**
     * 根据组织单元ID查找用户
     */
    @Select("SELECT * FROM users WHERE org_unit_id = #{orgUnitId} AND deleted = 0")
    List<UserPO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据组织单元ID列表查找用户
     */
    @Select("<script>" +
            "SELECT * FROM users WHERE org_unit_id IN " +
            "<foreach collection='orgUnitIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = 0" +
            "</script>")
    List<UserPO> findByOrgUnitIdIn(@Param("orgUnitIds") List<Long> orgUnitIds);

    /**
     * 分页查询用户
     */
    @Select("SELECT * FROM users WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<UserPO> findAllPaged(@Param("offset") int offset, @Param("size") int size);

    /**
     * 统计用户总数
     */
    @Select("SELECT COUNT(1) FROM users WHERE deleted = 0")
    long countAll();

    /**
     * 批量删除用户（逻辑删除）
     */
    @Select("<script>" +
            "UPDATE users SET deleted = 1, updated_at = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 条件分页查询用户
     */
    @Select("<script>" +
            "SELECT u.*, d.dept_name AS org_unit_name " +
            "FROM users u " +
            "LEFT JOIN departments d ON u.org_unit_id = d.id " +
            "WHERE u.deleted = 0 " +
            "<if test='username != null and username != \"\"'>" +
            "AND u.username LIKE CONCAT('%', #{username}, '%') " +
            "</if>" +
            "<if test='realName != null and realName != \"\"'>" +
            "AND u.real_name LIKE CONCAT('%', #{realName}, '%') " +
            "</if>" +
            "<if test='phone != null and phone != \"\"'>" +
            "AND u.phone LIKE CONCAT('%', #{phone}, '%') " +
            "</if>" +
            "<if test='orgUnitId != null'>" +
            "AND u.org_unit_id = #{orgUnitId} " +
            "</if>" +
            "<if test='status != null'>" +
            "AND u.status = #{status} " +
            "</if>" +
            "ORDER BY u.created_at DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<UserPO> findPagedWithConditions(
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("username") String username,
            @Param("realName") String realName,
            @Param("phone") String phone,
            @Param("orgUnitId") Long orgUnitId,
            @Param("status") Integer status);

    /**
     * 条件统计用户总数
     */
    @Select("<script>" +
            "SELECT COUNT(1) FROM users u WHERE u.deleted = 0 " +
            "<if test='username != null and username != \"\"'>" +
            "AND u.username LIKE CONCAT('%', #{username}, '%') " +
            "</if>" +
            "<if test='realName != null and realName != \"\"'>" +
            "AND u.real_name LIKE CONCAT('%', #{realName}, '%') " +
            "</if>" +
            "<if test='phone != null and phone != \"\"'>" +
            "AND u.phone LIKE CONCAT('%', #{phone}, '%') " +
            "</if>" +
            "<if test='orgUnitId != null'>" +
            "AND u.org_unit_id = #{orgUnitId} " +
            "</if>" +
            "<if test='status != null'>" +
            "AND u.status = #{status} " +
            "</if>" +
            "</script>")
    long countWithConditions(
            @Param("username") String username,
            @Param("realName") String realName,
            @Param("phone") String phone,
            @Param("orgUnitId") Long orgUnitId,
            @Param("status") Integer status);

    /**
     * 获取简单用户列表（用于选择器）
     */
    @Select("<script>" +
            "SELECT id, username, real_name, org_unit_id FROM users WHERE deleted = 0 AND status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR real_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY created_at DESC LIMIT 100" +
            "</script>")
    List<UserPO> findSimpleList(@Param("keyword") String keyword);

    /**
     * 根据用户ID查询角色ID列表
     */
    @Select("SELECT role_id FROM user_roles WHERE user_id = #{userId}")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询角色代码列表
     */
    @Select("SELECT r.role_code FROM roles r " +
            "INNER JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询权限代码列表
     */
    @Select("SELECT DISTINCT p.permission_code FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
            "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * 获取系统中所有权限代码（用于超级管理员）
     */
    @Select("SELECT DISTINCT permission_code FROM permissions WHERE permission_code IS NOT NULL")
    List<String> findAllPermissionCodes();

    /**
     * 根据用户ID查询角色名称列表
     */
    @Select("SELECT r.role_name FROM user_roles ur " +
            "JOIN roles r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的所有角色关联
     */
    @Delete("DELETE FROM user_roles WHERE user_id = #{userId}")
    void deleteUserRoles(@Param("userId") Long userId);

    /**
     * 插入用户角色关联
     */
    @Insert("INSERT INTO user_roles (user_id, role_id, created_at) VALUES (#{userId}, #{roleId}, NOW())")
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
