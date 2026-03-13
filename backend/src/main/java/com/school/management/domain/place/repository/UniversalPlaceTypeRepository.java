package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.entity.UniversalSpaceType;

import java.util.List;
import java.util.Optional;

/**
 * 通用空间类型仓储接口
 */
public interface UniversalSpaceTypeRepository {

    /**
     * 保存空间类型
     */
    UniversalSpaceType save(UniversalSpaceType spaceType);

    /**
     * 根据ID查询
     */
    Optional<UniversalSpaceType> findById(Long id);

    /**
     * 根据类型编码查询
     */
    Optional<UniversalSpaceType> findByTypeCode(String typeCode);

    /**
     * 查询所有空间类型
     */
    List<UniversalSpaceType> findAll();

    /**
     * 查询所有启用的空间类型
     */
    List<UniversalSpaceType> findAllEnabled();

    /**
     * 查询所有根类型
     */
    List<UniversalSpaceType> findAllRootTypes();

    /**
     * 查询允许的子类型
     */
    List<UniversalSpaceType> findAllowedChildTypes(String parentTypeCode);

    /**
     * 检查类型编码是否存在
     */
    boolean existsByTypeCode(String typeCode);

    /**
     * 检查类型是否被使用（有空间实例使用该类型）
     */
    boolean isTypeInUse(String typeCode);

    /**
     * 根据ID删除
     */
    void deleteById(Long id);

    /**
     * 批量查询
     */
    List<UniversalSpaceType> findByTypeCodes(List<String> typeCodes);
}
