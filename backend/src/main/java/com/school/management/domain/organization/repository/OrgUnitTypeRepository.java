package com.school.management.domain.organization.repository;

import com.school.management.domain.organization.model.entity.OrgType;

import java.util.List;
import java.util.Optional;

/**
 * 组织类型仓储接口
 */
public interface OrgUnitTypeRepository {

    OrgType save(OrgType orgType);

    Optional<OrgType> findById(Long id);

    Optional<OrgType> findByTypeCode(String typeCode);

    List<OrgType> findAll();

    List<OrgType> findAllEnabled();

    List<OrgType> findByParentTypeCode(String parentTypeCode);

    List<OrgType> findTopLevelTypes();

    List<OrgType> findByCategory(String category);

    List<OrgType> findByFeature(String featureKey);

    boolean existsByTypeCode(String typeCode);

    boolean isTypeInUse(String typeCode);

    void deleteById(Long id);
}
