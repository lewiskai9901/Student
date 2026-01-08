package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    User findByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询角色代码列表
     */
    @Select("""
            SELECT r.role_code
            FROM roles r
            INNER JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.deleted = 0
            """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询角色名称列表
     */
    @Select("""
            SELECT r.role_name
            FROM roles r
            INNER JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.deleted = 0
            """)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询角色ID列表
     */
    @Select("""
            SELECT ur.role_id
            FROM user_roles ur
            INNER JOIN roles r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            AND r.deleted = 0
            """)
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询权限代码列表
     */
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM permissions p
            INNER JOIN role_permissions rp ON p.id = rp.permission_id
            INNER JOIN user_roles ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM users WHERE phone = #{phone} AND deleted = 0")
    User findByPhone(@Param("phone") String phone);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username} AND deleted = 0 " +
            "AND (#{excludeId} IS NULL OR id != #{excludeId})")
    boolean existsByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);

    /**
     * 检查手机号是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE phone = #{phone} AND deleted = 0 " +
            "AND (#{excludeId} IS NULL OR id != #{excludeId})")
    boolean existsByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    User findByEmail(@Param("email") String email);

    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email} AND deleted = 0 " +
            "AND (#{excludeId} IS NULL OR id != #{excludeId})")
    boolean existsByEmail(@Param("email") String email, @Param("excludeId") Long excludeId);

    /**
     * 根据微信OpenID查询用户
     */
    @Select("SELECT * FROM users WHERE wechat_openid = #{openId} AND deleted = 0")
    User findByWechatOpenid(@Param("openId") String openId);

    /**
     * 检查微信OpenID是否已绑定
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE wechat_openid = #{openId} AND deleted = 0")
    boolean existsByWechatOpenid(@Param("openId") String openId);

    /**
     * 查询有微信openid的所有用户
     */
    @Select("SELECT * FROM users WHERE wechat_openid IS NOT NULL AND wechat_openid != '' AND deleted = 0 AND status = 1")
    List<User> findUsersWithOpenid();

    /**
     * 根据角色ID查询有微信openid的用户
     */
    @Select("""
            SELECT DISTINCT u.*
            FROM users u
            INNER JOIN user_roles ur ON u.id = ur.user_id
            WHERE ur.role_id IN (#{roleIds})
            AND u.wechat_openid IS NOT NULL
            AND u.wechat_openid != ''
            AND u.deleted = 0
            AND u.status = 1
            """)
    List<User> findUsersWithOpenidByRoleIds(@Param("roleIds") String roleIds);

    /**
     * 根据用户ID列表查询有微信openid的用户
     */
    @Select("""
            SELECT *
            FROM users
            WHERE id IN (#{userIds})
            AND wechat_openid IS NOT NULL
            AND wechat_openid != ''
            AND deleted = 0
            AND status = 1
            """)
    List<User> findUsersWithOpenidByIds(@Param("userIds") String userIds);
}