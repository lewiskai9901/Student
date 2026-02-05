package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.entity.SpaceType;

import java.util.List;
import java.util.Optional;

/**
 * 场所类型仓储接口（简化版）
 */
public interface SpaceTypeRepository {

    /**
     * 保存场所类型
     */
    SpaceType save(SpaceType spaceType);

    /**
     * 根据ID查询
     */
    Optional<SpaceType> findById(Long id);

    /**
     * 根据类型编码查询
     */
    Optional<SpaceType> findByTypeCode(String typeCode);

    /**
     * 查询所有场所类型
     */
    List<SpaceType> findAll();

    /**
     * 查询所有启用的类型
     */
    List<SpaceType> findAllEnabled();

    /**
     * 根据父类型编码查询子类型
     */
    List<SpaceType> findByParentTypeCode(String parentTypeCode);

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
