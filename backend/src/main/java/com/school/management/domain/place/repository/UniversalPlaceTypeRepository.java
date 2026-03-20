package com.school.management.domain.place.repository;

import com.school.management.domain.place.model.entity.UniversalPlaceType;

import java.util.List;
import java.util.Optional;

/**
 * 通用场所类型仓储接口（统一类型系统 Phase 3）
 */
public interface UniversalPlaceTypeRepository {

    UniversalPlaceType save(UniversalPlaceType placeType);

    Optional<UniversalPlaceType> findById(Long id);

    Optional<UniversalPlaceType> findByTypeCode(String typeCode);

    List<UniversalPlaceType> findAll();

    List<UniversalPlaceType> findAllEnabled();

    List<UniversalPlaceType> findAllRootTypes();

    List<UniversalPlaceType> findByParentTypeCode(String parentTypeCode);

    List<UniversalPlaceType> findByCategory(String category);

    boolean existsByTypeCode(String typeCode);

    boolean isTypeInUse(String typeCode);

    void deleteById(Long id);

    List<UniversalPlaceType> findByTypeCodes(List<String> typeCodes);

    List<UniversalPlaceType> findAllBaseCategories();

    List<UniversalPlaceType> findConcreteRootTypes();

    List<UniversalPlaceType> findAllConcreteTypes();
}
