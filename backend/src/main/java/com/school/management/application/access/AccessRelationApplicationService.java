package com.school.management.application.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.infrastructure.access.UserContextHolder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRelationApplicationService {

    private final AccessRelationRepository accessRelationRepository;
    private final UniversalPlaceRepository placeRepository;
    private final OrgUnitRepository orgUnitRepository;

    public List<AccessRelation> findByResource(String resourceType, Long resourceId) {
        List<AccessRelation> relations = accessRelationRepository.findByResource(resourceType, resourceId);
        enrichRelations(relations);
        return relations;
    }

    public List<AccessRelation> findBySubject(String subjectType, Long subjectId) {
        List<AccessRelation> relations = accessRelationRepository.findBySubject(subjectType, subjectId);
        enrichRelations(relations);
        return relations;
    }

    public List<AccessRelation> findBySubjectAndResourceType(String subjectType, Long subjectId, String resourceType) {
        List<AccessRelation> relations = accessRelationRepository.findBySubjectAndResourceType(subjectType, subjectId, resourceType);
        enrichRelations(relations);
        return relations;
    }

    /**
     * 填充关系的资源名称到 metadata（场所名称、组织名称等）
     */
    private void enrichRelations(List<AccessRelation> relations) {
        if (relations == null || relations.isEmpty()) return;

        for (AccessRelation rel : relations) {
            Map<String, Object> meta = rel.getMetadata() != null ? new HashMap<>(rel.getMetadata()) : new HashMap<>();
            try {
                if ("place".equals(rel.getResourceType()) && rel.getResourceId() != null) {
                    placeRepository.findById(rel.getResourceId()).ifPresent(place -> {
                        meta.put("placeName", place.getPlaceName());
                        meta.put("placeCode", place.getPlaceCode());
                    });
                } else if ("org_unit".equals(rel.getResourceType()) && rel.getResourceId() != null) {
                    orgUnitRepository.findById(rel.getResourceId()).ifPresent(org -> {
                        meta.put("orgUnitName", org.getUnitName());
                    });
                }
                // 同样填充 subject 名称
                if ("org_unit".equals(rel.getSubjectType()) && rel.getSubjectId() != null) {
                    if (!meta.containsKey("subjectName")) {
                        orgUnitRepository.findById(rel.getSubjectId()).ifPresent(org -> {
                            meta.put("subjectName", org.getUnitName());
                        });
                    }
                }
            } catch (Exception e) {
                log.warn("填充关系元数据失败: relationId={}, error={}", rel.getId(), e.getMessage());
            }
            rel.setMetadata(meta);
        }
    }

    public boolean checkAccess(String resourceType, Long resourceId, String relation, String subjectType, Long subjectId) {
        return accessRelationRepository.exists(resourceType, resourceId, relation, subjectType, subjectId);
    }

    @Transactional
    public AccessRelation create(CreateCommand cmd) {
        AccessRelation relation = AccessRelation.builder()
                .resourceType(cmd.getResourceType())
                .resourceId(cmd.getResourceId())
                .relation(cmd.getRelation())
                .subjectType(cmd.getSubjectType())
                .subjectId(cmd.getSubjectId())
                .includeChildren(cmd.isIncludeChildren())
                .accessLevel(cmd.getAccessLevel())
                .metadata(cmd.getMetadata())
                .remark(cmd.getRemark())
                .createdBy(UserContextHolder.getUserId())
                .build();
        return accessRelationRepository.save(relation);
    }

    @Transactional
    public void update(Long id, UpdateCommand cmd) {
        AccessRelation relation = accessRelationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));
        if (cmd.getRelation() != null) relation.setRelation(cmd.getRelation());
        if (cmd.getAccessLevel() != null) relation.setAccessLevel(cmd.getAccessLevel());
        if (cmd.getIncludeChildren() != null) relation.setIncludeChildren(cmd.getIncludeChildren());
        if (cmd.getMetadata() != null) {
            Map<String, Object> merged = relation.getMetadata() != null ? new HashMap<>(relation.getMetadata()) : new HashMap<>();
            merged.putAll(cmd.getMetadata());
            relation.setMetadata(merged);
        }
        if (cmd.getRemark() != null) relation.setRemark(cmd.getRemark());
        accessRelationRepository.update(relation);
    }

    @Transactional
    public void delete(Long id) {
        accessRelationRepository.deleteById(id);
    }

    @Transactional
    public int batchCreate(List<CreateCommand> commands) {
        List<AccessRelation> relations = commands.stream().map(cmd -> AccessRelation.builder()
                .resourceType(cmd.getResourceType())
                .resourceId(cmd.getResourceId())
                .relation(cmd.getRelation())
                .subjectType(cmd.getSubjectType())
                .subjectId(cmd.getSubjectId())
                .includeChildren(cmd.isIncludeChildren())
                .accessLevel(cmd.getAccessLevel())
                .metadata(cmd.getMetadata())
                .remark(cmd.getRemark())
                .createdBy(UserContextHolder.getUserId())
                .build()).toList();
        return accessRelationRepository.batchSave(relations);
    }

    @Transactional
    public int batchDelete(List<Long> ids) {
        return accessRelationRepository.batchDeleteByIds(ids);
    }

    // ---------- Command DTOs ----------

    @Data
    public static class CreateCommand {
        private String resourceType;
        private Long resourceId;
        private String relation;
        private String subjectType;
        private Long subjectId;
        private boolean includeChildren;
        private int accessLevel = 1;
        private Map<String, Object> metadata;
        private String remark;
    }

    @Data
    public static class UpdateCommand {
        private String relation;
        private Integer accessLevel;
        private Boolean includeChildren;
        private Map<String, Object> metadata;
        private String remark;
    }
}
