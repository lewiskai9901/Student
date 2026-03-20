package com.school.management.application.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
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

    public List<AccessRelation> findByResource(String resourceType, Long resourceId) {
        return accessRelationRepository.findByResource(resourceType, resourceId);
    }

    public List<AccessRelation> findBySubject(String subjectType, Long subjectId) {
        return accessRelationRepository.findBySubject(subjectType, subjectId);
    }

    public List<AccessRelation> findBySubjectAndResourceType(String subjectType, Long subjectId, String resourceType) {
        return accessRelationRepository.findBySubjectAndResourceType(subjectType, subjectId, resourceType);
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
