package com.school.management.domain.organization.repository;

import com.school.management.domain.organization.model.entity.OrgUnitTypeEntity;

import java.util.List;
import java.util.Optional;

/**
 * 组织类型仓储接口
 */
public interface OrgUnitTypeRepository {

    /**
     * 保存组织类型
     */
    OrgUnitTypeEntity save(OrgUnitTypeEntity orgUnitType);

    /**
     * 根据ID查询
     */
    Optional<OrgUnitTypeEntity> findById(Long id);

    /**
     * 根据类型编码查询
     */
    Optional<OrgUnitTypeEntity> findByTypeCode(String typeCode);

    /**
     * 查询所有组织类型
     */
    List<OrgUnitTypeEntity> findAll();

    /**
     * 查询所有启用的类型
     */
    List<OrgUnitTypeEntity> findAllEnabled();

    /**
     * 根据父类型编码查询子类型
     */
    List<OrgUnitTypeEntity> findByParentTypeCode(String parentTypeCode);

    /**
     * 查询顶级类型
     */
    List<OrgUnitTypeEntity> findTopLevelTypes();

    /**
     * 查询教学单位类型
     */
    List<OrgUnitTypeEntity> findAcademicTypes();

    /**
     * 查询职能部门类型
     */
    List<OrgUnitTypeEntity> findFunctionalTypes();

    /**
     * 查询可检查的类型
     */
    List<OrgUnitTypeEntity> findInspectableTypes();

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
