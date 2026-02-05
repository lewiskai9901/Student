package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.space.model.entity.SpaceCategory;
import com.school.management.domain.space.model.valueobject.SpaceLevel;
import com.school.management.domain.space.repository.SpaceCategoryRepository;
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
public class SpaceCategoryRepositoryImpl implements SpaceCategoryRepository {

    private final SpaceCategoryMapper mapper;
    private final SpaceCategoryDomainMapper domainMapper;

    @Override
    public SpaceCategory save(SpaceCategory category) {
        SpaceCategoryPO po = domainMapper.toPO(category);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        category.setId(po.getId());
        return category;
    }

    @Override
    public Optional<SpaceCategory> findById(Long id) {
        SpaceCategoryPO po = mapper.selectById(id);
        return Optional.ofNullable(domainMapper.toDomain(po));
    }

    @Override
    public Optional<SpaceCategory> findByCode(String categoryCode) {
        LambdaQueryWrapper<SpaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceCategoryPO::getCategoryCode, categoryCode);
        SpaceCategoryPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(domainMapper.toDomain(po));
    }

    @Override
    public List<SpaceCategory> findAll() {
        LambdaQueryWrapper<SpaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SpaceCategoryPO::getSortOrder)
               .orderByAsc(SpaceCategoryPO::getId);
        return mapper.selectList(wrapper).stream()
            .map(domainMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SpaceCategory> findByLevel(SpaceLevel level) {
        LambdaQueryWrapper<SpaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceCategoryPO::getApplyToLevel, level.name())
               .orderByAsc(SpaceCategoryPO::getSortOrder)
               .orderByAsc(SpaceCategoryPO::getId);
        return mapper.selectList(wrapper).stream()
            .map(domainMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SpaceCategory> findAllEnabled() {
        LambdaQueryWrapper<SpaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceCategoryPO::getIsEnabled, true)
               .orderByAsc(SpaceCategoryPO::getSortOrder)
               .orderByAsc(SpaceCategoryPO::getId);
        return mapper.selectList(wrapper).stream()
            .map(domainMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SpaceCategory> findEnabledByLevel(SpaceLevel level) {
        LambdaQueryWrapper<SpaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceCategoryPO::getApplyToLevel, level.name())
               .eq(SpaceCategoryPO::getIsEnabled, true)
               .orderByAsc(SpaceCategoryPO::getSortOrder)
               .orderByAsc(SpaceCategoryPO::getId);
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
        LambdaQueryWrapper<SpaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceCategoryPO::getCategoryCode, categoryCode);
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByCodeExcludeId(String categoryCode, Long excludeId) {
        LambdaQueryWrapper<SpaceCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceCategoryPO::getCategoryCode, categoryCode)
               .ne(SpaceCategoryPO::getId, excludeId);
        return mapper.selectCount(wrapper) > 0;
    }
}
