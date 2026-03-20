package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
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
public class UniversalPlaceRepositoryImpl implements UniversalPlaceRepository {

    private final UniversalPlaceMapper mapper;
    private final ObjectMapper objectMapper;

    public UniversalPlaceRepositoryImpl(UniversalPlaceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UniversalPlace save(UniversalPlace place) {
        UniversalPlacePO po = toPO(place);
        if (po.getId() == null) {
            mapper.insert(po);
            place.setId(po.getId());
            // 更新路径
            if (place.getParentId() == null) {
                place.setPath("/" + po.getId() + "/");
                place.setLevel(0);
            }
            po = toPO(place);
            mapper.updateById(po);
        } else {
            int rows = mapper.updateById(po);
            if (rows == 0) {
                throw new IllegalStateException("更新场所失败：未找到 ID=" + po.getId() + " 的记录");
            }
        }
        // 重新查询以获取最新数据（含关联字段）
        UniversalPlacePO saved = mapper.selectById(po.getId());
        return saved != null ? toDomain(saved) : toDomain(po);
    }

    @Override
    public Optional<UniversalPlace> findById(Long id) {
        UniversalPlacePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<UniversalPlace> findByPlaceCode(String placeCode) {
        UniversalPlacePO po = mapper.findByPlaceCode(placeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UniversalPlace> findAll() {
        return mapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlace> findAllRoots() {
        return mapper.findAllRoots().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlace> findChildren(Long parentId) {
        return mapper.findChildren(parentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlace> findDescendants(Long ancestorId) {
        Optional<UniversalPlace> ancestor = findById(ancestorId);
        if (ancestor.isEmpty()) {
            return List.of();
        }
        return findByPathPrefix(ancestor.get().getPath());
    }

    @Override
    public List<UniversalPlace> findByPathPrefix(String pathPrefix) {
        return mapper.findByPathPrefix(pathPrefix).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlace> findByTypeCode(String typeCode) {
        return mapper.findByTypeCode(typeCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlace> findByOrgUnitId(Long orgUnitId) {
        return mapper.findByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlace> findByResponsibleUserId(Long userId) {
        return mapper.findByResponsibleUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPlaceCode(String placeCode) {
        return mapper.existsByPlaceCode(placeCode);
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
    public int countByParentIdAndPlaceCode(Long parentId, String placeCode) {
        return mapper.countByParentIdAndPlaceCode(parentId, placeCode);
    }

    @Override
    public int countRootByPlaceCode(String placeCode) {
        return mapper.countRootByPlaceCode(placeCode);
    }

    @Override
    public int countChildren(Long parentId) {
        return mapper.countChildren(parentId);
    }

    @Override
    public int countDescendants(Long ancestorId) {
        Optional<UniversalPlace> ancestor = findById(ancestorId);
        if (ancestor.isEmpty()) {
            return 0;
        }
        return mapper.countDescendants(ancestorId, ancestor.get().getPath());
    }

    @Override
    public List<UniversalPlace> findPage(PlaceQueryCriteria criteria, int page, int size) {
        Page<UniversalPlacePO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UniversalPlacePO> wrapper = buildQueryWrapper(criteria);
        Page<UniversalPlacePO> result = mapper.selectPage(pageParam, wrapper);
        return result.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(PlaceQueryCriteria criteria) {
        LambdaQueryWrapper<UniversalPlacePO> wrapper = buildQueryWrapper(criteria);
        return mapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<UniversalPlacePO> buildQueryWrapper(PlaceQueryCriteria criteria) {
        LambdaQueryWrapper<UniversalPlacePO> wrapper = new LambdaQueryWrapper<>();
        if (criteria != null) {
            if (criteria.getTypeCode() != null) {
                wrapper.eq(UniversalPlacePO::getTypeCode, criteria.getTypeCode());
            }
            if (criteria.getParentId() != null) {
                wrapper.eq(UniversalPlacePO::getParentId, criteria.getParentId());
            }
            if (criteria.getOrgUnitId() != null) {
                wrapper.eq(UniversalPlacePO::getOrgUnitId, criteria.getOrgUnitId());
            }
            if (criteria.getStatus() != null) {
                wrapper.eq(UniversalPlacePO::getStatus, criteria.getStatus());
            }
            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                wrapper.and(w -> w.like(UniversalPlacePO::getPlaceName, criteria.getKeyword())
                        .or().like(UniversalPlacePO::getPlaceCode, criteria.getKeyword()));
            }
        }
        wrapper.orderByAsc(UniversalPlacePO::getLevel, UniversalPlacePO::getPlaceName);
        return wrapper;
    }

    // ==================== 转换方法 ====================

    private UniversalPlacePO toPO(UniversalPlace entity) {
        UniversalPlacePO po = new UniversalPlacePO();
        po.setId(entity.getId());
        po.setPlaceCode(entity.getPlaceCode());
        po.setPlaceName(entity.getPlaceName());
        po.setTypeCode(entity.getTypeCode());
        po.setDescription(entity.getDescription());
        po.setParentId(entity.getParentId());
        po.setPath(entity.getPath());
        po.setLevel(entity.getLevel());
        po.setCapacity(entity.getCapacity());
        po.setCurrentOccupancy(entity.getCurrentOccupancy());
        po.setOrgUnitId(entity.getOrgUnitId());
        po.setResponsibleUserId(entity.getResponsibleUserId());
        po.setGender(entity.getGender());
        po.setStatus(entity.getStatus() != null ? entity.getStatus().getCode() : PlaceStatus.NORMAL.getCode());

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

    private UniversalPlace toDomain(UniversalPlacePO po) {
        Map<String, Object> attributes = new HashMap<>();
        if (po.getAttributes() != null && !po.getAttributes().isBlank()) {
            try {
                attributes = objectMapper.readValue(po.getAttributes(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                // 忽略解析错误
            }
        }

        return UniversalPlace.builder()
                .id(po.getId())
                .placeCode(po.getPlaceCode())
                .placeName(po.getPlaceName())
                .typeCode(po.getTypeCode())
                .description(po.getDescription())
                .parentId(po.getParentId())
                .path(po.getPath())
                .level(po.getLevel() != null ? po.getLevel() : 0)
                .capacity(po.getCapacity())
                .currentOccupancy(po.getCurrentOccupancy() != null ? po.getCurrentOccupancy() : 0)
                .orgUnitId(po.getOrgUnitId())
                .responsibleUserId(po.getResponsibleUserId())
                .gender(po.getGender())
                .status(PlaceStatus.fromCode(po.getStatus()))
                .attributes(attributes)
                .build();
    }
}
