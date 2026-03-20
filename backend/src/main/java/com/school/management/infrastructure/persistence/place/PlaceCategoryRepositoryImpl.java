package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.place.model.entity.PlaceCategory;
import com.school.management.domain.place.model.valueobject.PlaceLevel;
import com.school.management.domain.place.repository.PlaceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 空间分类仓储实现
 */
@Repository
@RequiredArgsConstructor
public class PlaceCategoryRepositoryImpl implements PlaceCategoryRepository {

    private final PlaceCategoryMapper mapper;
    private final PlaceCategoryDomainMapper domainMapper;

    @Override
    public PlaceCategory save(PlaceCategory category) {
        PlaceCategoryPO po = domainMapper.toPO(category);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        category.setId(po.getId());
        return category;
    }

    @Override
    public Optional<PlaceCategory> findById(Long id) {
        PlaceCategoryPO po = mapper.selectById(id);
        return Optional.ofNullable(domainMapper.toDomain(po));
    }

    @Override
    public Optional<PlaceCategory> findByCode(String categoryCode) {
        LambdaQueryWrapper<PlaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaceCategoryPO::getCategoryCode, categoryCode);
        PlaceCategoryPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(domainMapper.toDomain(po));
    }

    @Override
    public List<PlaceCategory> findAll() {
        LambdaQueryWrapper<PlaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PlaceCategoryPO::getSortOrder)
               .orderByAsc(PlaceCategoryPO::getId);
        return mapper.selectList(wrapper).stream()
            .map(domainMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<PlaceCategory> findByLevel(PlaceLevel level) {
        LambdaQueryWrapper<PlaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaceCategoryPO::getApplyToLevel, level.name())
               .orderByAsc(PlaceCategoryPO::getSortOrder)
               .orderByAsc(PlaceCategoryPO::getId);
        return mapper.selectList(wrapper).stream()
            .map(domainMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<PlaceCategory> findAllEnabled() {
        LambdaQueryWrapper<PlaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaceCategoryPO::getIsEnabled, true)
               .orderByAsc(PlaceCategoryPO::getSortOrder)
               .orderByAsc(PlaceCategoryPO::getId);
        return mapper.selectList(wrapper).stream()
            .map(domainMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<PlaceCategory> findEnabledByLevel(PlaceLevel level) {
        LambdaQueryWrapper<PlaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaceCategoryPO::getApplyToLevel, level.name())
               .eq(PlaceCategoryPO::getIsEnabled, true)
               .orderByAsc(PlaceCategoryPO::getSortOrder)
               .orderByAsc(PlaceCategoryPO::getId);
        return mapper.selectList(wrapper).stream()
            .map(domainMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public boolean existsByCode(String categoryCode) {
        LambdaQueryWrapper<PlaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaceCategoryPO::getCategoryCode, categoryCode);
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByCodeExcludeId(String categoryCode, Long excludeId) {
        LambdaQueryWrapper<PlaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaceCategoryPO::getCategoryCode, categoryCode)
               .ne(PlaceCategoryPO::getId, excludeId);
        return mapper.selectCount(wrapper) > 0;
    }
}
