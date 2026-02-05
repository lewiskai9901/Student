package com.school.management.domain.user.repository;

import com.school.management.domain.user.model.entity.UserType;

import java.util.List;
import java.util.Optional;

/**
 * 用户类型仓储接口
 */
public interface UserTypeRepository {

    /**
     * 保存用户类型
     */
    UserType save(UserType userType);

    /**
     * 根据ID查询
     */
    Optional<UserType> findById(Long id);

    /**
     * 根据类型编码查询
     */
    Optional<UserType> findByTypeCode(String typeCode);

    /**
     * 查询所有用户类型
     */
    List<UserType> findAll();

    /**
     * 查询所有启用的类型
     */
    List<UserType> findAllEnabled();

    /**
     * 根据父类型编码查询子类型
     */
    List<UserType> findByParentTypeCode(String parentTypeCode);

    /**
     * 查询顶级类型
     */
    List<UserType> findTopLevelTypes();

    /**
     * 查询可登录的类型
     */
    List<UserType> findLoginableTypes();

    /**
     * 查询可作为检查员的类型
     */
    List<UserType> findInspectorTypes();

    /**
     * 查询可被检查的类型
     */
    List<UserType> findInspectableTypes();

    /**
     * 查询需要班级归属的类型
     */
    List<UserType> findClassRequiredTypes();

    /**
     * 检查类型编码是否存在
     */
    boolean existsByTypeCode(String typeCode);

    /**
     * 检查类型是否被使用
     */
    boolean isTypeInUse(String typeCode);

    /**
     * 根据ID删除
     */
    void deleteById(Long id);
}
