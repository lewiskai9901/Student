package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AccessRelationRepositoryImpl implements AccessRelationRepository {

    private final AccessRelationMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<AccessRelation> findById(Long id) {
        AccessRelationPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AccessRelation> findByResource(String resourceType, Long resourceId) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getResourceType, resourceType)
                .eq(AccessRelationPO::getResourceId, resourceId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AccessRelation> findByResourceAndRelation(String resourceType, Long resourceId, String relation) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getResourceType, resourceType)
                .eq(AccessRelationPO::getResourceId, resourceId)
                .eq(AccessRelationPO::getRelation, relation);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AccessRelation> findBySubject(String subjectType, Long subjectId) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getSubjectType, subjectType)
                .eq(AccessRelationPO::getSubjectId, subjectId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AccessRelation> findBySubjectAndResourceType(String subjectType, Long subjectId, String resourceType) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getSubjectType, subjectType)
                .eq(AccessRelationPO::getSubjectId, subjectId)
                .eq(AccessRelationPO::getResourceType, resourceType);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean exists(String resourceType, Long resourceId, String relation, String subjectType, Long subjectId) {
        return mapper.existsRelation(resourceType, resourceId, relation, subjectType, subjectId);
    }

    @Override
    public List<Long> findAccessibleResourceIds(String resourceType, String subjectType, Long subjectId) {
        return mapper.findAccessibleResourceIds(resourceType, subjectType, subjectId);
    }

    @Override
    public List<Long> findAccessibleResourceIdsByOrgUnits(String resourceType, List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) return Collections.emptyList();
        return mapper.findAccessibleResourceIdsByOrgUnits(resourceType, orgUnitIds);
    }

    @Override
    public AccessRelation save(AccessRelation relation) {
        AccessRelationPO po = toPO(relation);
        mapper.insert(po);
        relation.setId(po.getId());
        return relation;
    }

    @Override
    public void update(AccessRelation relation) {
        AccessRelationPO po = toPO(relation);
        mapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByResource(String resourceType, Long resourceId) {
        LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
                .eq(AccessRelationPO::getResourceType, resourceType)
                .eq(AccessRelationPO::getResourceId, resourceId);
        mapper.delete(wrapper);
    }

    @Override
    public int batchSave(List<AccessRelation> relations) {
        int count = 0;
        for (AccessRelation rel : relations) {
            save(rel);
            count++;
        }
        return count;
    }

    @Override
    public int batchDeleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return mapper.deleteBatchIds(ids);
    }

    // ---------- 映射方法 ----------

    private AccessRelation toDomain(AccessRelationPO po) {
        Map<String, Object> meta = null;
        if (po.getMetadata() != null && !po.getMetadata().isEmpty()) {
            try {
                meta = objectMapper.readValue(po.getMetadata(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("解析 metadata JSON 失败, id={}: {}", po.getId(), e.getMessage());
            }
        }

        return AccessRelation.builder()
                .id(po.getId())
                .resourceType(po.getResourceType())
                .resourceId(po.getResourceId())
                .relation(po.getRelation())
                .subjectType(po.getSubjectType())
                .subjectId(po.getSubjectId())
                .includeChildren(Boolean.TRUE.equals(po.getIncludeChildren()))
                .accessLevel(po.getAccessLevel() != null ? po.getAccessLevel() : 1)
                .metadata(meta)
                .remark(po.getRemark())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private AccessRelationPO toPO(AccessRelation domain) {
        AccessRelationPO po = new AccessRelationPO();
        po.setId(domain.getId());
        po.setResourceType(domain.getResourceType());
        po.setResourceId(domain.getResourceId());
        po.setRelation(domain.getRelation());
        po.setSubjectType(domain.getSubjectType());
        po.setSubjectId(domain.getSubjectId());
        po.setIncludeChildren(domain.isIncludeChildren());
        po.setAccessLevel(domain.getAccessLevel());
        po.setRemark(domain.getRemark());
        po.setCreatedBy(domain.getCreatedBy());

        if (domain.getMetadata() != null && !domain.getMetadata().isEmpty()) {
            try {
                po.setMetadata(objectMapper.writeValueAsString(domain.getMetadata()));
            } catch (Exception e) {
                log.warn("序列化 metadata 失败: {}", e.getMessage());
            }
        }

        return po;
    }
}
