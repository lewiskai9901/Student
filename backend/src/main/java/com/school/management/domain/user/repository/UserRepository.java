package com.school.management.domain.user.repository;

import com.school.management.domain.shared.Repository;
import com.school.management.domain.user.model.aggregate.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口
 */
public interface UserRepository extends Repository<User, Long> {

    /**
     * 保存用户
     */
    User save(User user);

    /**
     * 根据ID查找用户
     */
    Optional<User> findById(Long id);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据工号查找用户
     */
    Optional<User> findByEmployeeNo(String employeeNo);

    /**
     * 根据微信OpenID查找用户
     */
    Optional<User> findByWechatOpenid(String openid);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查用户名是否存在（排除指定ID）
     */
    boolean existsByUsernameAndIdNot(String username, Long excludeId);

    /**
     * 根据组织单元ID查找用户列表
     */
    List<User> findByOrgUnitId(Long orgUnitId);

    /**
     * 根据组织单元ID列表查找用户列表
     */
    List<User> findByOrgUnitIdIn(List<Long> orgUnitIds);

    /**
     * 删除用户
     */
    void deleteById(Long id);

    /**
     * 批量删除用户
     */
    void deleteByIds(List<Long> ids);

    /**
     * 分页查询用户
     */
    List<User> findAll(int page, int size);

    /**
     * 查询用户总数
     */
    long count();

    /**
     * 条件分页查询用户
     */
    List<User> findPagedWithConditions(int page, int size, String username, String realName,
                                       String phone, Long orgUnitId, Integer status);

    /**
     * 条件统计用户总数
     */
    long countWithConditions(String username, String realName, String phone,
                            Long orgUnitId, Integer status);

    /**
     * 获取简单用户列表
     */
    List<User> findSimpleList(String keyword);

    /**
     * 获取所有用户
     */
    List<User> findAllUsers();

    /**
     * 根据ID列表批量查找用户
     */
    List<User> findByIds(List<Long> ids);
}
