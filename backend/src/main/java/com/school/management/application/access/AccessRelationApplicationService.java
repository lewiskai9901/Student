package com.school.management.application.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.model.valueobject.AccessLevel;
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

import java.time.LocalDateTime;
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
    /** create/delete 统一委托 grant/revoke (双路径收敛 2026-06-13) — 注册校验/审批路由/
     *  schema 校验/幂等/基数/事实事件/缓存失效 全部由 AccessRelationService 单点保障。 */
    private final AccessRelationService accessRelationService;
    private final MetadataSchemaValidator metadataSchemaValidator;
    private final AccessCheckCache checkCache;

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
     * 解析用户归属的 org_unit ID 列表 — 仅取 MEMBER_OF / OWNER_OF / MANAGES 关系.
     *
     * <p>供数据范围过滤用 (如"我收到的检查"按所在组织过滤). 放在
     * application.access 内, 让 inspection 等模块不直访 AccessRelationRepository
     * (NoBypassAuthServiceTest 守护).
     */
    public List<Long> resolveOrgUnitIds(Long userId) {
        if (userId == null) return List.of();
        List<AccessRelation> rels = accessRelationRepository.findBySubjectAndResourceType(
                "USER", userId, "ORG_UNIT");
        List<Long> ids = new java.util.ArrayList<>(rels.size());
        for (AccessRelation r : rels) {
            if ("MEMBER_OF".equalsIgnoreCase(r.getRelation())
                || "OWNER_OF".equalsIgnoreCase(r.getRelation())
                || "MANAGES".equalsIgnoreCase(r.getRelation())) {
                ids.add(r.getResourceId());
            }
        }
        return ids;
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

    /**
     * 创建关系 — 统一委托 {@link AccessRelationService#grant} (create vs grant 双路径收敛,
     * 2026-06-13)。由此 CRUD 路径获得与内部 grant 完全一致的保障链:
     * relation 注册校验 → 审批路由 → metadata schema 校验 → 幂等 → 基数强制 →
     * 关系事实事件 → check 缓存失效。Policy hook (BEFORE/AFTER_GRANT) 仍在本层。
     *
     * <p>审批关系 (approval_required=1): grant 返回负 pendingId, 此时返回未持久化回执
     * (id=负值, remark 说明), 关系待审批通过后才落 access_relations。
     */
    @Transactional
    public AccessRelation create(CreateCommand cmd) {
        // Policy hook — BEFORE_GRANT (grant = 授予关系, 即 create AccessRelation)
        // 例: 家属监护必须身份证校验 / 禁止给离职用户授权
        policyRegistry.enforce(new PolicyContext<>("access_relation", "BEFORE_GRANT", cmd));

        Long id = accessRelationService.grant(toGrantRequest(cmd));
        if (id < 0) {
            return AccessRelation.builder()
                    .id(id)
                    .resourceType(cmd.getResourceType()).resourceId(cmd.getResourceId())
                    .relation(cmd.getRelation())
                    .subjectType(cmd.getSubjectType()).subjectId(cmd.getSubjectId())
                    .remark("已提交审批 (审批单 #" + Math.abs(id) + "), 审批通过后生效")
                    .build();
        }
        AccessRelation saved = accessRelationRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("grant 返回的关系不存在: " + id));

        // Policy hook — AFTER_GRANT WARN/INFO 仅记日志 (审计/通知)
        List<Violation> warns = policyRegistry.check(
                new PolicyContext<>("access_relation", "AFTER_GRANT", saved));
        warns.forEach(w -> log.warn("[Policy/{}] {}: {}", w.severity(), w.code(), w.message()));

        return saved;
    }

    private AccessRelationService.GrantRequest toGrantRequest(CreateCommand cmd) {
        AccessRelationService.GrantRequest r = AccessRelationService.GrantRequest.of(
                cmd.getSubjectType(), cmd.getSubjectId(), cmd.getRelation(),
                cmd.getResourceType(), cmd.getResourceId());
        r.accessLevel = cmd.getAccessLevel();
        r.metadata = cmd.getMetadata();
        r.validFrom = cmd.getValidFrom();
        r.validTo = cmd.getValidTo();
        r.remark = cmd.getRemark();
        r.grantedBy = UserContextHolder.getUserId();
        return r;
    }

    @Transactional
    public void update(Long id, UpdateCommand cmd) {
        AccessRelation relation = accessRelationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));
        if (cmd.getRelation() != null) relation.setRelation(cmd.getRelation());
        if (cmd.getAccessLevel() != null) relation.setAccessLevel(cmd.getAccessLevel());
        if (cmd.getMetadata() != null) {
            Map<String, Object> merged = relation.getMetadata() != null ? new HashMap<>(relation.getMetadata()) : new HashMap<>();
            merged.putAll(cmd.getMetadata());
            // 双路径收敛: 合并后整体过 schema 校验, 与 grant 路径一致
            metadataSchemaValidator.validate(relation.getRelation(), merged);
            relation.setMetadata(merged);
        }
        if (cmd.getValidFrom() != null) relation.setValidFrom(cmd.getValidFrom());
        if (cmd.getValidTo() != null) relation.setValidTo(cmd.getValidTo());
        if (cmd.getRemark() != null) relation.setRemark(cmd.getRemark());
        accessRelationRepository.update(relation);

        // accessLevel/validTo 变更影响授权判定 — 与 grant/revoke 对称失效 check 缓存
        checkCache.invalidateBySubject(relation.getSubjectType(), relation.getSubjectId());
        checkCache.invalidateByResource(relation.getResourceType(), relation.getResourceId());
    }

    /**
     * 删除关系 — 统一委托 {@link AccessRelationService#revoke} (按 tuple 撤销;
     * active_uniq 保证活跃 tuple 唯一)。由此获得: history 归档(含 HTTP 上下文) +
     * valid_to 截断 + 撤销事实事件 + check 缓存失效。
     */
    @Transactional
    public void delete(Long id) {
        // Policy hook — BEFORE_REVOKE (revoke = 撤销关系, 即 delete AccessRelation)
        policyRegistry.enforce(new PolicyContext<>("access_relation", "BEFORE_REVOKE", id));

        AccessRelation existing = accessRelationRepository.findById(id).orElse(null);
        if (existing == null) {
            log.warn("[AccessRelation] delete: 关系不存在或已删除 id={}", id);
            return;
        }

        accessRelationService.revoke(AccessRelationService.RevokeRequest.of(
                existing.getSubjectType(), existing.getSubjectId(),
                existing.getRelation(), existing.getResourceType(), existing.getResourceId(),
                "关系管理删除", UserContextHolder.getUserId()));

        // Policy hook — AFTER_REVOKE WARN/INFO 仅记日志
        List<Violation> warns = policyRegistry.check(
                new PolicyContext<>("access_relation", "AFTER_REVOKE", existing));
        warns.forEach(w -> log.warn("[Policy/{}] {}: {}", w.severity(), w.code(), w.message()));
    }

    /** 批量创建 — 逐条走 grant (保障链同 {@link #create}); 进入审批队列的不计入返回值。 */
    @Transactional
    public int batchCreate(List<CreateCommand> commands) {
        int created = 0;
        for (CreateCommand cmd : commands) {
            Long id = accessRelationService.grant(toGrantRequest(cmd));
            if (id > 0) created++;
        }
        return created;
    }

    /** 批量删除 — 逐条走 revoke (归档/事件/缓存同 {@link #delete}); 不存在的 id 跳过。 */
    @Transactional
    public int batchDelete(List<Long> ids) {
        int revoked = 0;
        for (Long id : ids) {
            AccessRelation existing = accessRelationRepository.findById(id).orElse(null);
            if (existing == null) continue;
            accessRelationService.revoke(AccessRelationService.RevokeRequest.of(
                    existing.getSubjectType(), existing.getSubjectId(),
                    existing.getRelation(), existing.getResourceType(), existing.getResourceId(),
                    "关系管理批量删除", UserContextHolder.getUserId()));
            revoked++;
        }
        return revoked;
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
        private AccessLevel accessLevel = AccessLevel.FULL;
        private Map<String, Object> metadata;
        private LocalDateTime validFrom;
        private LocalDateTime validTo;
        private String remark;
    }

    @Data
    public static class UpdateCommand {
        private String relation;
        private AccessLevel accessLevel;
        private Map<String, Object> metadata;
        private LocalDateTime validFrom;
        private LocalDateTime validTo;
        private String remark;
    }
}
