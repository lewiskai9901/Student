package com.school.management.application.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.PolicyRegistry;
import com.school.management.infrastructure.extension.Violation;
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
    private final UserRepository userRepository;
    private final PolicyRegistry policyRegistry;

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
                } else if ("user".equals(rel.getResourceType()) && rel.getResourceId() != null) {
                    userRepository.findById(rel.getResourceId()).ifPresent(user -> {
                        meta.put("userName", user.getRealName());
                        meta.put("username", user.getUsername());
                    });
                }
                // subject 名称
                if ("org_unit".equals(rel.getSubjectType()) && rel.getSubjectId() != null) {
                    if (!meta.containsKey("subjectName")) {
                        orgUnitRepository.findById(rel.getSubjectId()).ifPresent(org -> {
                            meta.put("subjectName", org.getUnitName());
                        });
                    }
                } else if ("user".equals(rel.getSubjectType()) && rel.getSubjectId() != null) {
                    if (!meta.containsKey("subjectName")) {
                        userRepository.findById(rel.getSubjectId()).ifPresent(user -> {
                            meta.put("subjectName", user.getRealName());
                            meta.put("username", user.getUsername());
                        });
                    }
                } else if ("place".equals(rel.getSubjectType()) && rel.getSubjectId() != null) {
                    if (!meta.containsKey("subjectName")) {
                        placeRepository.findById(rel.getSubjectId()).ifPresent(place -> {
                            meta.put("subjectName", place.getPlaceName());
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
        // Policy hook — BEFORE_GRANT (grant = 授予关系, 即 create AccessRelation)
        // 例: 家属监护必须身份证校验 / 禁止给离职用户授权
        policyRegistry.enforce(new PolicyContext<>("access_relation", "BEFORE_GRANT", cmd));

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
        AccessRelation saved = accessRelationRepository.save(relation);

        // Policy hook — AFTER_GRANT WARN/INFO 仅记日志 (审计/通知)
        List<Violation> warns = policyRegistry.check(
                new PolicyContext<>("access_relation", "AFTER_GRANT", saved));
        warns.forEach(w -> log.warn("[Policy/{}] {}: {}", w.severity(), w.code(), w.message()));

        return saved;
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
        // Policy hook — BEFORE_REVOKE (revoke = 撤销关系, 即 delete AccessRelation)
        policyRegistry.enforce(new PolicyContext<>("access_relation", "BEFORE_REVOKE", id));

        // 在删除前读出 relation, 以便 AFTER 能拿到 payload
        AccessRelation existing = accessRelationRepository.findById(id).orElse(null);

        accessRelationRepository.deleteById(id);

        // Policy hook — AFTER_REVOKE WARN/INFO 仅记日志
        List<Violation> warns = policyRegistry.check(
                new PolicyContext<>("access_relation", "AFTER_REVOKE", existing));
        warns.forEach(w -> log.warn("[Policy/{}] {}: {}", w.severity(), w.code(), w.message()));
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

    public PagedResult listPaged(String resourceType, String subjectType, String relation,
                                  int page, int size) {
        List<AccessRelation> all = accessRelationRepository.listFiltered(resourceType, subjectType, relation);
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(all.size(), from + size);
        List<AccessRelation> slice = from < all.size() ? all.subList(from, to) : List.of();
        enrichRelations(slice);
        return new PagedResult(slice, (long) all.size());
    }

    public record PagedResult(List<AccessRelation> records, Long total) {}

    @Data
    public static class CreateCommand {
        private String resourceType;
        private Long resourceId;
        private String relation;
        private String subjectType;
        private Long subjectId;
        private boolean includeChildren;
        private String accessLevel = "FULL";
        private Map<String, Object> metadata;
        private String remark;
    }

    @Data
    public static class UpdateCommand {
        private String relation;
        private String accessLevel;
        private Boolean includeChildren;
        private Map<String, Object> metadata;
        private String remark;
    }
}
