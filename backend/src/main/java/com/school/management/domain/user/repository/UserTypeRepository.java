package com.school.management.domain.user.repository;

import com.school.management.domain.user.model.entity.UserType;

import java.util.List;
import java.util.Optional;

/**
 * 用户类型仓储接口（统一类型系统 Phase 2）
 */
public interface UserTypeRepository {

    UserType save(UserType userType);

    Optional<UserType> findById(Long id);

    Optional<UserType> findByTypeCode(String typeCode);

    List<UserType> findAll();

    List<UserType> findAllEnabled();

    List<UserType> findByParentTypeCode(String parentTypeCode);

    List<UserType> findTopLevelTypes();

    List<UserType> findByCategory(String category);

    List<UserType> findByFeature(String featureKey);

    boolean existsByTypeCode(String typeCode);

    boolean isTypeInUse(String typeCode);

    void deleteById(Long id);
}
