package com.school.management.infrastructure.persistence.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
     * 根据组织单元ID查找用户（通过 primary_org_unit_id）
     */
    @Select("SELECT * FROM users WHERE primary_org_unit_id = #{orgUnitId} AND deleted = 0")
    List<UserPO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据组织单元ID列表查找用户（通过 primary_org_unit_id）
     */
    @Select("<script>" +
            "SELECT * FROM users WHERE primary_org_unit_id IN " +
            "<foreach collection='orgUnitIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = 0" +
            "</script>")
    List<UserPO> findByOrgUnitIdIn(@Param("orgUnitIds") List<Long> orgUnitIds);

    /**
     * 根据用户类型编码查找所有用户
     */
    @Select("SELECT * FROM users WHERE user_type_code = #{userTypeCode} AND deleted = 0")
    List<UserPO> findByUserTypeCode(@Param("userTypeCode") String userTypeCode);

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
     * 统计某组织的归属成员数（primary_org_unit_id）
     */
    @Select("SELECT COUNT(1) FROM users WHERE primary_org_unit_id = #{orgUnitId} AND deleted = 0")
    long countByPrimaryOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 按用户类型统计某组织的归属成员数
     */
    @Select("SELECT user_type_code, COUNT(1) AS cnt FROM users " +
            "WHERE primary_org_unit_id = #{orgUnitId} AND deleted = 0 " +
            "GROUP BY user_type_code")
    List<java.util.Map<String, Object>> countByPrimaryOrgUnitIdGroupByType(@Param("orgUnitId") Long orgUnitId);

    /**
     * 单个逻辑删除用户（deleted 存 id，避免唯一键冲突）
     */
    @Update("UPDATE users SET deleted = id, updated_at = NOW() WHERE id = #{id} AND deleted = 0")
    int softDeleteById(@Param("id") Long id);

    /**
     * 批量删除用户（逻辑删除）
     */
    @Update("<script>" +
            "UPDATE users SET deleted = id, updated_at = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = 0" +
            "</script>")
    void deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 条件分页查询用户（通过 primary_org_unit_id 关联组织）
     */
    @Select("<script>" +
            "SELECT u.*, u.primary_org_unit_id AS org_unit_id, ou.unit_name AS org_unit_name " +
            "FROM users u " +
            "LEFT JOIN org_units ou ON u.primary_org_unit_id = ou.id AND ou.deleted = 0 " +
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
            "AND u.primary_org_unit_id = #{orgUnitId} " +
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
     * 条件统计用户总数（通过 primary_org_unit_id 关联组织）
     */
    @Select("<script>" +
            "SELECT COUNT(1) FROM users u " +
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
            "AND u.primary_org_unit_id = #{orgUnitId} " +
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
            "SELECT u.id, u.username, u.real_name, u.gender, u.user_type_code, " +
            "u.primary_org_unit_id, o.unit_name AS org_unit_name " +
            "FROM users u " +
            "LEFT JOIN org_units o ON u.primary_org_unit_id = o.id AND o.deleted = 0 " +
            "WHERE u.deleted = 0 AND u.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.real_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY u.created_at DESC LIMIT 100" +
            "</script>")
    List<UserPO> findSimpleList(@Param("keyword") String keyword);

    /**
     * 根据用户ID查询角色ID列表（过滤已过期和已停用的角色分配）
     */
    @Select("SELECT role_id FROM user_roles WHERE user_id = #{userId} " +
            "AND is_active = 1 AND (expires_at IS NULL OR expires_at > NOW())")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询角色代码列表（过滤已过期和已停用的角色分配）
     */
    @Select("SELECT r.role_code FROM roles r " +
            "INNER JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0 " +
            "AND ur.is_active = 1 AND (ur.expires_at IS NULL OR ur.expires_at > NOW())")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询权限代码列表（过滤已过期和已停用的角色分配）
     */
    @Select("SELECT DISTINCT p.permission_code FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
            "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND ur.is_active = 1 AND (ur.expires_at IS NULL OR ur.expires_at > NOW())")
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * 获取系统中所有权限代码（用于超级管理员）
     */
    @Select("SELECT DISTINCT permission_code FROM permissions WHERE permission_code IS NOT NULL")
    List<String> findAllPermissionCodes();

    /**
     * 根据用户ID查询角色名称列表（过滤已过期和已停用的角色分配）
     */
    @Select("SELECT r.role_name FROM user_roles ur " +
            "JOIN roles r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0 " +
            "AND ur.is_active = 1 AND (ur.expires_at IS NULL OR ur.expires_at > NOW())")
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    /**
     * 批量查询多用户的角色（避免 N+1 查询）
     * 返回字段：user_id、role_id、role_name
     */
    @Select("<script>" +
            "SELECT ur.user_id, r.id AS role_id, r.role_name " +
            "FROM user_roles ur " +
            "LEFT JOIN roles r ON ur.role_id = r.id AND r.deleted = 0 " +
            "WHERE ur.user_id IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach> " +
            "AND ur.is_active = 1 AND (ur.expires_at IS NULL OR ur.expires_at > NOW())" +
            "</script>")
    List<java.util.Map<String, Object>> findRolesByUserIds(@Param("userIds") List<Long> userIds);

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

    /**
     * 清除某组织下所有用户的归属关系（primary_org_unit_id 设为 NULL）
     */
    @Update("UPDATE users SET primary_org_unit_id = NULL, updated_at = NOW() " +
            "WHERE primary_org_unit_id = #{orgUnitId} AND deleted = 0")
    int clearPrimaryOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 设置用户的归属组织（primary_org_unit_id）
     */
    @Update("UPDATE users SET primary_org_unit_id = #{orgUnitId}, updated_at = NOW() " +
            "WHERE id = #{userId} AND deleted = 0")
    int setPrimaryOrgUnitId(@Param("userId") Long userId, @Param("orgUnitId") Long orgUnitId);

    /**
     * 清除指定用户的归属组织（仅当匹配 orgUnitId 时）
     */
    @Update("UPDATE users SET primary_org_unit_id = NULL, updated_at = NOW() " +
            "WHERE id = #{userId} AND primary_org_unit_id = #{orgUnitId} AND deleted = 0")
    int clearPrimaryOrgUnitIdForUser(@Param("userId") Long userId, @Param("orgUnitId") Long orgUnitId);
}
