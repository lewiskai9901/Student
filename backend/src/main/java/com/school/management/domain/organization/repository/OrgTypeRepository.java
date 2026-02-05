package com.school.management.domain.organization.repository;

import com.school.management.domain.organization.model.entity.OrgType;

import java.util.List;
import java.util.Optional;

/**
 * 组织类型仓储接口
 */
public interface OrgTypeRepository {

    /**
     * 保存组织类型
     */
    OrgType save(OrgType orgType);

    /**
     * 根据ID查询
     */
    Optional<OrgType> findById(Long id);

    /**
     * 根据类型编码查询
     */
    Optional<OrgType> findByTypeCode(String typeCode);

    /**
     * 查询所有类型
     */
    List<OrgType> findAll();

    /**
     * 查询所有启用的类型
     */
    List<OrgType> findAllEnabled();

    /**
     * 查询顶级类型
     */
    List<OrgType> findTopLevelTypes();

    /**
     * 查询子类型
     */
    List<OrgType> findByParentTypeCode(String parentTypeCode);

    /**
     * 查询指定层级的类型
     */
    List<OrgType> findByLevelOrder(Integer levelOrder);

    /**
     * 检查类型编码是否存在
     */
    boolean existsByTypeCode(String typeCode);

    /**
     * 检查类型是否被使用
     */
    boolean isTypeInUse(String typeCode);

    /**
     * 删除类型
     */
    void deleteById(Long id);
}
