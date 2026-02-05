package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.space.model.aggregate.UniversalSpace;
import com.school.management.domain.space.model.valueobject.SpaceStatus;
import com.school.management.domain.space.repository.UniversalSpaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通用空间仓储实现
 */
@Repository
public class UniversalSpaceRepositoryImpl implements UniversalSpaceRepository {

    private final UniversalSpaceMapper mapper;
    private final ObjectMapper objectMapper;

    public UniversalSpaceRepositoryImpl(UniversalSpaceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UniversalSpace save(UniversalSpace space) {
        UniversalSpacePO po = toPO(space);
        if (po.getId() == null) {
            mapper.insert(po);
            space.setId(po.getId());
            // 更新路径
            if (space.getParentId() == null) {
                space.setPath("/" + po.getId() + "/");
                space.setLevel(0);
            }
            po = toPO(space);
            mapper.updateById(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<UniversalSpace> findById(Long id) {
        UniversalSpacePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<UniversalSpace> findBySpaceCode(String spaceCode) {
        UniversalSpacePO po = mapper.findBySpaceCode(spaceCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UniversalSpace> findAll() {
        return mapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpace> findAllRoots() {
        return mapper.findAllRoots().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpace> findChildren(Long parentId) {
        return mapper.findChildren(parentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpace> findDescendants(Long ancestorId) {
        Optional<UniversalSpace> ancestor = findById(ancestorId);
        if (ancestor.isEmpty()) {
            return List.of();
        }
        return findByPathPrefix(ancestor.get().getPath());
    }

    @Override
    public List<UniversalSpace> findByPathPrefix(String pathPrefix) {
        return mapper.findByPathPrefix(pathPrefix).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpace> findByTypeCode(String typeCode) {
        return mapper.findByTypeCode(typeCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpace> findByOrgUnitId(Long orgUnitId) {
        return mapper.findByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpace> findByResponsibleUserId(Long userId) {
        return mapper.findByResponsibleUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySpaceCode(String spaceCode) {
        return mapper.existsBySpaceCode(spaceCode);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            mapper.deleteBatchIds(ids);
        }
    }

    @Override
    public int countChildren(Long parentId) {
        return mapper.countChildren(parentId);
    }

    @Override
    public int countDescendants(Long ancestorId) {
        Optional<UniversalSpace> ancestor = findById(ancestorId);
        if (ancestor.isEmpty()) {
            return 0;
        }
        return mapper.countDescendants(ancestorId, ancestor.get().getPath());
    }

    @Override
    public List<UniversalSpace> findPage(SpaceQueryCriteria criteria, int page, int size) {
        Page<UniversalSpacePO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UniversalSpacePO> wrapper = buildQueryWrapper(criteria);
        Page<UniversalSpacePO> result = mapper.selectPage(pageParam, wrapper);
        return result.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(SpaceQueryCriteria criteria) {
        LambdaQueryWrapper<UniversalSpacePO> wrapper = buildQueryWrapper(criteria);
        return mapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<UniversalSpacePO> buildQueryWrapper(SpaceQueryCriteria criteria) {
        LambdaQueryWrapper<UniversalSpacePO> wrapper = new LambdaQueryWrapper<>();
        if (criteria != null) {
            if (criteria.getTypeCode() != null) {
                wrapper.eq(UniversalSpacePO::getTypeCode, criteria.getTypeCode());
            }
            if (criteria.getParentId() != null) {
                wrapper.eq(UniversalSpacePO::getParentId, criteria.getParentId());
            }
            if (criteria.getOrgUnitId() != null) {
                wrapper.eq(UniversalSpacePO::getOrgUnitId, criteria.getOrgUnitId());
            }
            if (criteria.getStatus() != null) {
                wrapper.eq(UniversalSpacePO::getStatus, criteria.getStatus());
            }
            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                wrapper.and(w -> w.like(UniversalSpacePO::getSpaceName, criteria.getKeyword())
                        .or().like(UniversalSpacePO::getSpaceCode, criteria.getKeyword()));
            }
        }
        wrapper.orderByAsc(UniversalSpacePO::getLevel, UniversalSpacePO::getSpaceName);
        return wrapper;
    }

    // ==================== 转换方法 ====================

    private UniversalSpacePO toPO(UniversalSpace entity) {
        UniversalSpacePO po = new UniversalSpacePO();
        po.setId(entity.getId());
        po.setSpaceCode(entity.getSpaceCode());
        po.setSpaceName(entity.getSpaceName());
        po.setTypeCode(entity.getTypeCode());
        po.setDescription(entity.getDescription());
        po.setParentId(entity.getParentId());
        po.setPath(entity.getPath());
        po.setLevel(entity.getLevel());
        po.setCapacity(entity.getCapacity());
        po.setCurrentOccupancy(entity.getCurrentOccupancy());
        po.setOrgUnitId(entity.getOrgUnitId());
        po.setResponsibleUserId(entity.getResponsibleUserId());
        po.setStatus(entity.getStatus() != null ? entity.getStatus().getCode() : SpaceStatus.NORMAL.getCode());

        // 转换 attributes 为 JSON
        if (entity.getAttributes() != null && !entity.getAttributes().isEmpty()) {
            try {
                po.setAttributes(objectMapper.writeValueAsString(entity.getAttributes()));
            } catch (JsonProcessingException e) {
                po.setAttributes("{}");
            }
        } else {
            po.setAttributes("{}");
        }

        return po;
    }

    private UniversalSpace toDomain(UniversalSpacePO po) {
        Map<String, Object> attributes = new HashMap<>();
        if (po.getAttributes() != null && !po.getAttributes().isBlank()) {
            try {
                attributes = objectMapper.readValue(po.getAttributes(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                // 忽略解析错误
            }
        }

        return UniversalSpace.builder()
                .id(po.getId())
                .spaceCode(po.getSpaceCode())
                .spaceName(po.getSpaceName())
                .typeCode(po.getTypeCode())
                .description(po.getDescription())
                .parentId(po.getParentId())
                .path(po.getPath())
                .level(po.getLevel() != null ? po.getLevel() : 0)
                .capacity(po.getCapacity())
                .currentOccupancy(po.getCurrentOccupancy() != null ? po.getCurrentOccupancy() : 0)
                .orgUnitId(po.getOrgUnitId())
                .responsibleUserId(po.getResponsibleUserId())
                .status(SpaceStatus.fromCode(po.getStatus()))
                .attributes(attributes)
                .build();
    }
}
