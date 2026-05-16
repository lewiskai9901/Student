package com.school.management.application.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.web.RequestContextHelper;
import com.school.management.domain.access.event.RelationApprovalRequestedEvent;
import com.school.management.domain.access.model.entity.PendingRelationApproval;
import com.school.management.domain.access.model.valueobject.AccessLevel;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.access.repository.AccessRelationRepository.DirectRelationRef;
import com.school.management.domain.access.repository.AccessRelationRepository.InsertDirectCommand;
import com.school.management.domain.access.repository.AccessRelationRepository.RelationEdgeRef;
import com.school.management.infrastructure.extension.RelationTypeDef.Implied;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一访问关系应用服务 (Zanzibar 风格 6 API).
 *
 * <p>历史名 {@code AuthorizationService} 与 {@link com.school.management.domain.access.service.AuthorizationService}
 * (RBAC 接口) 重名, W1.3 起改名为 {@code AccessRelationService}, 语义更准确 — 它操作的就是 access_relations.
 *
 * <p>所有业务代码查询关系**必须**走这里, 不要直接拼 SQL。底层 SQL 已收回到
 * {@link AccessRelationRepository}, 此服务只做编排 + implied BFS。
 *
 * 语义:
 *   - check        (subject, relation, resource, at?): boolean
 *   - checkAt      同上, 带时间点
 *   - expand       (resource, relation) → 所有 subject
 *   - lookup       (subject, relation, resource_type) → 所有 resource
 *   - grant        (subject, relation, resource, ...) → 创建关系 + 发事件
 *   - revoke       (subject, relation, resource, reason) → 软删 + 归档 + 发事件
 *   - batchCheck   (List<Check>)
 *
 * 时间语义:
 *   valid_from / valid_to 形成"生效窗口"。
 *   check() 默认只看"当前活跃" (valid_to IS NULL OR valid_to > NOW())。
 *   checkAt(..., timestamp) 看该时间点的快照。
 *
 * 软删语义:
 *   deleted = 1 → 数据纠错, 彻底排除 (包括历史查询)。
 *   valid_to 到期 → 业务失效, 保留在主表/history 供审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRelationService {

    private final AccessRelationRepository repo;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final List<RelationDiscoveryRule> discoveryRules;
    /** 仅 implied 缓存 refresh 时读 relation_types 配置表用; 业务查询全部走 repo. */
    private final JdbcTemplate jdbcTemplate;
    /** check() 结果 Redis 缓存 (Phase 4 W4.2): 60s TTL, grant/revoke 主动 invalidate. */
    private final AccessCheckCache checkCache;
    /** metadata 字段 schema 校验 (Phase 4 W4.3 骨架). */
    private final MetadataSchemaValidator metadataSchemaValidator;
    /** 审批流路由 (Phase 7 W7.1) — 关系类型 approval_required=1 时走两步 grant. */
    private final RelationApprovalService relationApprovalService;

    /**
     * Phase 6 (workflow-engine): 审批引擎模式切换. INLINE = pending_relation_approvals 表 (默认),
     * FLOWABLE = 走 Flowable 流程引擎 (要求 RelationApprovalWorkflowService bean 存在).
     */
    @Value("${access.approval.engine:INLINE}")
    private String approvalEngineMode;

    /** Phase 6: 仅 FLOWABLE 模式下使用 — Flowable autoconfig 未启用时为 null. */
    @Autowired(required = false)
    private RelationApprovalWorkflowService relationApprovalWorkflowService;

    /**
     * 关系推导索引: (targetType + targetRelation) → 所有可能派生出它的
     * (sourceRelation, fromType, Implied) 三元组。启动加载, 变更时 refresh。
     */
    private final Map<String, List<ImpliedSource>> impliedIndex = new ConcurrentHashMap<>();
    /**
     * 正向推导索引: (sourceFromType + sourceRelation) → 所有"由它派生出"的
     * (targetType, targetRelation, discoveryRule) 三元组。BFS 展开时使用。
     */
    private final Map<String, List<ImpliedTarget>> impliedOutgoing = new ConcurrentHashMap<>();
    /** discoveryRule code → 实现 */
    private final Map<String, RelationDiscoveryRule> discoveryByCode = new ConcurrentHashMap<>();

    /** BFS 展开 implied 的最大深度 (防止恶意/错误声明造成的无限环) */
    static final int MAX_IMPLIED_DEPTH = 5;

    @PostConstruct
    void bootImpliedCache() {
        for (RelationDiscoveryRule r : discoveryRules) {
            discoveryByCode.put(r.code(), r);
        }
        refreshImpliedCache();
        log.info("[AccessRelation] implied relation cache ready — {} reverse entries, {} forward entries, {} discovery rules",
            impliedIndex.size(), impliedOutgoing.size(), discoveryByCode.size());
    }

    /** 重新从 relation_types.implied_relations 加载索引 (DDL 变更后调用) */
    public void refreshImpliedCache() {
        Map<String, List<ImpliedSource>> freshReverse = new HashMap<>();
        Map<String, List<ImpliedTarget>> freshForward = new HashMap<>();
        // 这条查询直接读 relation_types 配置表 (不在 access_relations 上), 保留 jdbcTemplate.
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT relation_code, from_type, to_type, implied_relations FROM relation_types " +
            "WHERE is_enabled = 1 AND implied_relations IS NOT NULL");
        TypeReference<List<Implied>> typeRef = new TypeReference<>() {};
        for (Map<String, Object> row : rows) {
            String impliedJson = (String) row.get("implied_relations");
            if (impliedJson == null || impliedJson.isBlank()) continue;
            List<Implied> implied;
            try {
                implied = objectMapper.readValue(impliedJson, typeRef);
            } catch (Exception e) {
                log.warn("[AccessRelation] skip malformed implied_relations for {}: {}",
                    row.get("relation_code"), e.getMessage());
                continue;
            }
            String fromType = (String) row.get("from_type");
            String sourceRelation = (String) row.get("relation_code");
            String sourceToType = (String) row.get("to_type");
            for (Implied i : implied) {
                String revKey = impliedKey(i.targetType(), i.relation());
                freshReverse.computeIfAbsent(revKey, k -> new ArrayList<>())
                    .add(new ImpliedSource(fromType, sourceRelation, sourceToType, i));
                String fwdKey = impliedKey(sourceToType, sourceRelation);
                freshForward.computeIfAbsent(fwdKey, k -> new ArrayList<>())
                    .add(new ImpliedTarget(i.targetType(), i.relation(), i.discoveryRule()));
            }
        }
        impliedIndex.clear();
        impliedIndex.putAll(freshReverse);
        impliedOutgoing.clear();
        impliedOutgoing.putAll(freshForward);
    }

    private static String impliedKey(String typeOrResource, String relation) {
        return typeOrResource + "#" + relation;
    }

    // ═══════════════════════════════════════════════════════════════
    // check / checkAt
    // ═══════════════════════════════════════════════════════════════

    /**
     * 当前时间点:某主体对某资源是否有某关系? (包含 implied 推导)
     *
     * <p>结果走 Redis 缓存 (Phase 4 W4.2), TTL 默认 60s. grant/revoke 时主动失效.
     * 注意: {@link #checkAt(String, Long, String, String, Long, LocalDateTime)}
     * 不缓存 — 时间点快照查询语义不适合缓存.
     */
    public boolean check(String subjectType, Long subjectId,
                         String relation,
                         String resourceType, Long resourceId) {
        return checkCache.checkCached(subjectType, subjectId, relation, resourceType, resourceId,
            () -> checkAt(subjectType, subjectId, relation, resourceType, resourceId, LocalDateTime.now()));
    }

    /** 指定时间点:某主体对某资源是否有某关系? (包含 implied 推导) */
    public boolean checkAt(String subjectType, Long subjectId,
                           String relation,
                           String resourceType, Long resourceId,
                           LocalDateTime at) {
        // 1. 直接关系
        if (checkDirect(subjectType, subjectId, relation, resourceType, resourceId, at)) {
            return true;
        }
        // 2. 展开 implied: 是否有任何"上游关系"能推导到 (resourceType, resourceId, relation) ?
        return checkImplied(subjectType, subjectId, relation, resourceType, resourceId, at);
    }

    /** 仅查直接写在 access_relations 表的关系 (不展开推导) */
    public boolean checkDirect(String subjectType, Long subjectId,
                               String relation,
                               String resourceType, Long resourceId,
                               LocalDateTime at) {
        return repo.checkDirectActive(subjectType, subjectId, relation, resourceType, resourceId, at);
    }

    /**
     * 展开 implied — 多层 BFS 递归版.
     *
     * 思路 (Zanzibar 标准):
     *  1. worklist 放 (resourceType, resourceId, relation) 三元组, 代表
     *     "subject 对此 resource 有此 relation (直接或已展开得到)".
     *  2. 初始化: 查 subject 所有直接关系作为起点.
     *  3. 每轮取一个节点:
     *     - 若匹配 target (resourceType, resourceId, relation) → 返 true.
     *     - 否则查 impliedOutgoing 找派生规则, 通过 discoveryRule 展开得到一批新节点,
     *       压入 worklist (depth+1).
     *  4. visited set 防环, MAX_IMPLIED_DEPTH 防恶意死循环.
     */
    private boolean checkImplied(String subjectType, Long subjectId,
                                  String relation,
                                  String resourceType, Long resourceId,
                                  LocalDateTime at) {
        if (impliedOutgoing.isEmpty()) return false;

        // 1. 起点: subject 所有直接关系
        List<DirectRelationRef> directRows = repo.findActiveDirectRelationsBySubject(subjectType, subjectId, at);

        Deque<WorkItem> work = new ArrayDeque<>(directRows.size() + 8);
        Set<String> visited = new HashSet<>();
        for (DirectRelationRef row : directRows) {
            String rt = row.resourceType();
            Long rid = row.resourceId();
            String rel = row.relation();
            if (rt == null || rid == null || rel == null) continue;
            String key = rt + ":" + rid + "#" + rel;
            if (visited.add(key)) {
                work.add(new WorkItem(rt, rid, rel, 0));
            }
        }

        // 2. BFS 展开
        while (!work.isEmpty()) {
            WorkItem cur = work.poll();
            // 命中
            if (cur.resourceType.equals(resourceType)
                && cur.relation.equals(relation)
                && cur.resourceId.equals(resourceId)) {
                log.debug("[AccessRelation] implied 命中 (depth={}): {}:{} -> {}:{} {}",
                    cur.depth, subjectType, subjectId, resourceType, resourceId, relation);
                return true;
            }
            // 深度限制
            if (cur.depth >= MAX_IMPLIED_DEPTH) {
                log.warn("[AccessRelation] implied BFS 深度超限 ({}), subject={}:{} stop at {}:{} {}",
                    MAX_IMPLIED_DEPTH, subjectType, subjectId, cur.resourceType, cur.resourceId, cur.relation);
                continue;
            }
            // 展开派生
            List<ImpliedTarget> derivations = impliedOutgoing.get(impliedKey(cur.resourceType, cur.relation));
            if (derivations == null || derivations.isEmpty()) continue;
            for (ImpliedTarget d : derivations) {
                RelationDiscoveryRule rule = discoveryByCode.get(d.discoveryRule());
                if (rule == null) {
                    log.debug("[AccessRelation] implied 跳过 — unknown discoveryRule: {}", d.discoveryRule());
                    continue;
                }
                List<Long> derivedIds = rule.discover(cur.resourceType, cur.resourceId);
                if (derivedIds == null || derivedIds.isEmpty()) continue;
                for (Long did : derivedIds) {
                    String key = d.targetType() + ":" + did + "#" + d.relation();
                    if (visited.add(key)) {
                        work.add(new WorkItem(d.targetType(), did, d.relation(), cur.depth + 1));
                    }
                }
            }
        }
        return false;
    }

    /** BFS 节点 */
    private record WorkItem(String resourceType, Long resourceId, String relation, int depth) {}

    /** 反向索引: 一条推导规则的源端信息 (targetType+relation → sources) */
    private record ImpliedSource(String fromType, String sourceRelation,
                                  String sourceToType, Implied implied) {}

    /** 正向索引: 从 (sourceToType + sourceRelation) 派生得到的 target 描述 */
    private record ImpliedTarget(String targetType, String relation, String discoveryRule) {}

    /** 批量判定(消息扇出等批处理场景) */
    public Map<String, Boolean> batchCheck(List<CheckRequest> requests) {
        Map<String, Boolean> result = new LinkedHashMap<>(requests.size());
        for (CheckRequest r : requests) {
            boolean ok = check(r.subjectType, r.subjectId, r.relation, r.resourceType, r.resourceId);
            result.put(r.key(), ok);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // expand:某资源 + 某关系 → 所有 subject
    // ═══════════════════════════════════════════════════════════════

    /** 某资源 + 某关系 → 授权的 subject 列表(只返 active) */
    public List<SubjectRef> expand(String resourceType, Long resourceId, String relation) {
        return repo.expandSubjects(resourceType, resourceId, relation).stream()
            .map(p -> new SubjectRef(p.subjectType(), p.subjectId(), p.accessLevel(),
                p.validFrom(), p.validTo()))
            .toList();
    }

    /**
     * 找对给定 resource 拥有 relation 关系的所有 subject id (含 implied 派生).
     */
    public List<Long> findSubjectsWithRelation(String resourceType, Long resourceId,
                                                String relation, boolean expandImplied) {
        return findSubjectsWithRelation(resourceType, resourceId, relation, null, expandImplied);
    }

    /**
     * 带 subject_type 过滤的版本 — 通常调用方希望只拿 "user" 主体 (如消息派发).
     *
     * @param subjectTypeFilter 若非 null, 仅返回 subject_type = 此值的 subject; null 则不过滤
     */
    public List<Long> findSubjectsWithRelation(String resourceType, Long resourceId,
                                                String relation, String subjectTypeFilter,
                                                boolean expandImplied) {
        // 1. 直查
        List<Long> direct = repo.findActiveSubjectIds(resourceType, resourceId, relation, subjectTypeFilter);
        if (!expandImplied) return direct;

        Set<Long> result = new LinkedHashSet<>(direct);

        // 2. 反向展开 implied (一层)
        List<ImpliedSource> sources = impliedIndex.get(impliedKey(resourceType, relation));
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>(result);
        }

        for (ImpliedSource s : sources) {
            if (subjectTypeFilter != null && !subjectTypeFilter.equals(s.fromType())) continue;

            RelationDiscoveryRule rule = discoveryByCode.get(s.implied().discoveryRule());
            if (rule == null) {
                log.debug("[AccessRelation] findSubjectsWithRelation: 跳过未知 discoveryRule={}",
                    s.implied().discoveryRule());
                continue;
            }
            List<Long> sourceResourceIds = rule.reverseDiscover(resourceType, resourceId);
            if (sourceResourceIds == null || sourceResourceIds.isEmpty()) continue;

            for (Long srcResId : sourceResourceIds) {
                List<Long> implied = repo.findActiveSubjectIds(
                    s.sourceToType(), srcResId, s.sourceRelation(), s.fromType());
                result.addAll(implied);
            }
        }
        return new ArrayList<>(result);
    }

    /** 某资源上的所有关系(不限 relation) */
    public List<RelationEdge> expandAll(String resourceType, Long resourceId) {
        return repo.expandAllOnResource(resourceType, resourceId).stream()
            .map(this::toRelationEdge)
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // lookup:某 subject + 某关系 → 所有 resource
    // ═══════════════════════════════════════════════════════════════

    /** 某用户 + 某关系 + 某资源类型 → 用户能访问的资源 ID 列表 */
    public List<Long> lookup(String subjectType, Long subjectId,
                             String relation, String resourceType) {
        return repo.lookupActiveResources(subjectType, subjectId, relation, resourceType).stream()
            .map(RelationEdgeRef::resourceId)
            .distinct()
            .toList();
    }

    /** 某用户所有关系(整张画像) */
    public List<RelationEdge> lookupAll(String subjectType, Long subjectId) {
        return repo.lookupAllForSubject(subjectType, subjectId).stream()
            .map(this::toRelationEdge)
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // grant / revoke
    // ═══════════════════════════════════════════════════════════════

    /**
     * 创建关系 → 发 RelationAssigned 事件.
     *
     * <p>Phase 7 W7.1: 当 {@code relation_types.approval_required=1} 时,
     * 不直接 INSERT 而是走两步审批流:
     * <ol>
     *   <li>写 {@code pending_relation_approvals} status=PENDING</li>
     *   <li>发 {@link RelationApprovalRequestedEvent}</li>
     *   <li>返回 <b>负数</b> {@code -pendingId} 表示"审批中,未真正生效"</li>
     * </ol>
     * 审批人调 {@code POST /approvals/{id}/approve} 后, 控制器再调
     * {@link #applyApprovedRequest(Long, Long)} 接通真正落库.
     */
    @Transactional
    public Long grant(GrantRequest r) {
        // 1. 校验 relation_types 是否存在且启用
        if (!repo.isRelationRegistered(r.relation, r.subjectType, r.resourceType)) {
            throw new IllegalArgumentException(String.format(
                "[AccessRelation] 非法 relation: %s %s -> %s (未注册或未启用)",
                r.relation, r.subjectType, r.resourceType));
        }

        // 1a. 审批路由 — 该关系类型需要审批 → 写 pending 表, 不落 access_relations
        if (relationApprovalService.requiresApproval(r.relation)) {
            // Phase 6 (workflow-engine): 升级路径 — 切换到 Flowable 流程引擎
            if ("FLOWABLE".equalsIgnoreCase(approvalEngineMode) && relationApprovalWorkflowService != null) {
                String processInstanceId = relationApprovalWorkflowService.startApproval(r);
                log.info("[AccessRelation] grant 转入 Flowable 审批流: relation={} {}:{} -> {}:{} processInstance={}",
                    r.relation, r.subjectType, r.subjectId, r.resourceType, r.resourceId, processInstanceId);
                // 负数约定与 INLINE 兼容: PID 是字符串, 用 hashCode 转 long 后取负
                return -Math.abs((long) processInstanceId.hashCode());
            }

            // 默认 INLINE 路径
            log.info("[AccessRelation] grant 转入审批流: relation={} {}:{} -> {}:{}",
                r.relation, r.subjectType, r.subjectId, r.resourceType, r.resourceId);
            Long pendingId = relationApprovalService.requestApproval(toPending(r));
            eventPublisher.publishEvent(new RelationApprovalRequestedEvent(
                pendingId, r.resourceType, r.resourceId, r.relation,
                r.subjectType, r.subjectId, r.grantedBy));
            // 负数返回值约定: 调用方按 Math.abs(...) 拿 pendingId, < 0 表示进入审批队列
            return -pendingId;
        }

        // 1b. 不需审批 → 走非审批分支 (forceGrant 复用)
        return forceGrant(r);
    }

    /**
     * 内部方法: 不走审批检查的 grant. 仅用于 {@link #applyApprovedRequest(Long, Long)}
     * 与 {@link #grant(GrantRequest)} 非审批分支共用底层 INSERT 逻辑.
     */
    @Transactional
    public Long forceGrant(GrantRequest r) {
        // 校验 metadata 满足关系的 schema 要求
        metadataSchemaValidator.validate(r.relation, r.metadata);

        // 幂等检查
        Optional<Long> existing = repo.findActiveByTuple(
            r.resourceType, r.resourceId, r.relation, r.subjectType, r.subjectId);
        if (existing.isPresent()) {
            log.info("[AccessRelation] forceGrant 幂等命中,返回现有关系 id={}", existing.get());
            return existing.get();
        }

        String metaJson;
        try {
            metaJson = r.metadata != null ? objectMapper.writeValueAsString(r.metadata) : null;
        } catch (Exception e) {
            metaJson = null;
        }
        Long newId = repo.insertDirect(new InsertDirectCommand(
            r.resourceType, r.resourceId, r.relation,
            r.subjectType, r.subjectId,
            r.includeChildren,
            r.accessLevel != null ? r.accessLevel : AccessLevel.FULL,
            r.validFrom, r.validTo,
            metaJson, r.remark,
            r.tenantId != null ? r.tenantId : 1L,
            r.grantedBy));
        log.info("[AccessRelation] grant: relation={} {} {}:{} -> {}:{} id={}",
            r.relation, r.subjectType, r.subjectId, r.resourceType, r.resourceId, r.accessLevel, newId);

        eventPublisher.publishEvent(new RelationAssignedEvent(
            newId, r.resourceType, r.resourceId, r.relation,
            r.subjectType, r.subjectId, r.grantedBy));

        checkCache.invalidateBySubject(r.subjectType, r.subjectId);
        checkCache.invalidateByResource(r.resourceType, r.resourceId);

        return newId;
    }

    /**
     * 审批通过后接通真正 grant.
     * 调用方先 {@link RelationApprovalService#approve(Long, Long)}, 再调此方法.
     */
    @Transactional
    public Long applyApprovedRequest(Long pendingId, Long approverId) {
        Optional<PendingRelationApproval> pendingOpt = relationApprovalService.findById(pendingId);
        if (pendingOpt.isEmpty()) {
            throw new IllegalArgumentException("pending 记录不存在: " + pendingId);
        }
        PendingRelationApproval p = pendingOpt.get();
        if (p.getStatus() != PendingRelationApproval.Status.APPROVED) {
            throw new IllegalStateException("pending 状态非 APPROVED: " + p.getStatus());
        }

        Long newId = forceGrant(toGrantRequest(p, approverId));
        relationApprovalService.linkGrantedRelation(pendingId, newId);
        return newId;
    }

    private PendingRelationApproval toPending(GrantRequest r) {
        return PendingRelationApproval.builder()
            .resourceType(r.resourceType).resourceId(r.resourceId).relation(r.relation)
            .subjectType(r.subjectType).subjectId(r.subjectId)
            .accessLevel(r.accessLevel).validFrom(r.validFrom).validTo(r.validTo)
            .metadata(r.metadata).remark(r.remark)
            .status(PendingRelationApproval.Status.PENDING)
            .requestedBy(r.grantedBy)
            .tenantId(r.tenantId != null ? r.tenantId : 1L)
            .build();
    }

    private GrantRequest toGrantRequest(PendingRelationApproval p, Long approverId) {
        GrantRequest r = GrantRequest.of(p.getSubjectType(), p.getSubjectId(),
            p.getRelation(), p.getResourceType(), p.getResourceId());
        r.accessLevel = p.getAccessLevel();
        r.validFrom = p.getValidFrom();
        r.validTo = p.getValidTo();
        r.metadata = p.getMetadata();
        r.remark = p.getRemark();
        r.grantedBy = p.getRequestedBy();  // 审批落库时 createdBy=申请人, approverId 仅用于审计
        r.tenantId = p.getTenantId();
        return r;
    }

    /**
     * 撤销关系:软删 + 归档 + 发事件.
     * Phase 7 W7.3: 抓 HTTP 上下文写到 history.operator_ip / operator_user_agent.
     */
    @Transactional
    public void revoke(RevokeRequest r) {
        // 1. 查到关系 id
        List<Long> ids = repo.findActiveIdsByTuple(
            r.resourceType, r.resourceId, r.relation, r.subjectType, r.subjectId);
        if (ids.isEmpty()) {
            log.warn("[AccessRelation] revoke 未找到活跃关系: {} {} -> {}:{}",
                r.relation, r.subjectId, r.resourceType, r.resourceId);
            return;
        }

        // 抓当前 HTTP 上下文 (非 web 调用 — 如事件 listener — 返 null, 不影响落库)
        String ip = RequestContextHelper.clientIp();
        String ua = RequestContextHelper.userAgent();

        for (Long id : ids) {
            repo.archiveAndSoftDelete(id, r.reason, r.revokedBy, ip, ua);
            eventPublisher.publishEvent(new RelationRevokedEvent(
                id, r.resourceType, r.resourceId, r.relation,
                r.subjectType, r.subjectId, r.reason, r.revokedBy));
            log.info("[AccessRelation] revoke id={} relation={} {} -> {}:{} reason={} ip={}",
                id, r.relation, r.subjectId, r.resourceType, r.resourceId, r.reason, ip);
        }

        // 失效 check() 缓存 (按 subject + resource 双向清, 与 grant 对称)
        checkCache.invalidateBySubject(r.subjectType, r.subjectId);
        checkCache.invalidateByResource(r.resourceType, r.resourceId);
    }

    // ═══════════════════════════════════════════════════════════════
    // 投影映射
    // ═══════════════════════════════════════════════════════════════

    private RelationEdge toRelationEdge(RelationEdgeRef ref) {
        return new RelationEdge(
            ref.relation(),
            ref.subjectType(), ref.subjectId(),
            ref.resourceType(), ref.resourceId(),
            ref.accessLevel(),
            ref.validFrom(), ref.validTo());
    }

    // ═══════════════════════════════════════════════════════════════
    // Data classes
    // ═══════════════════════════════════════════════════════════════

    public record SubjectRef(String subjectType, Long subjectId, AccessLevel accessLevel,
                             LocalDateTime validFrom, LocalDateTime validTo) {}

    public record RelationEdge(String relation,
                               String subjectType, Long subjectId,
                               String resourceType, Long resourceId,
                               AccessLevel accessLevel,
                               LocalDateTime validFrom, LocalDateTime validTo) {}

    public record CheckRequest(String subjectType, Long subjectId,
                               String relation,
                               String resourceType, Long resourceId) {
        public String key() {
            return String.format("%s:%d-[%s]->%s:%d", subjectType, subjectId, relation, resourceType, resourceId);
        }
    }

    public static class GrantRequest {
        public String subjectType; public Long subjectId;
        public String relation;
        public String resourceType; public Long resourceId;
        public AccessLevel accessLevel;                  // READ_ONLY / FULL / OWNER
        /** @deprecated 已废弃 — 传递性走 RelationTypeDef.isTransitive,详见 ADR-002 */
        @Deprecated
        public boolean includeChildren = false;
        public LocalDateTime validFrom; public LocalDateTime validTo;
        public Map<String, Object> metadata;
        public String remark;
        public Long grantedBy;
        public Long tenantId;

        public static GrantRequest of(String subjectType, Long subjectId,
                                      String relation,
                                      String resourceType, Long resourceId) {
            GrantRequest r = new GrantRequest();
            r.subjectType = subjectType; r.subjectId = subjectId;
            r.relation = relation;
            r.resourceType = resourceType; r.resourceId = resourceId;
            return r;
        }
    }

    public static class RevokeRequest {
        public String subjectType; public Long subjectId;
        public String relation;
        public String resourceType; public Long resourceId;
        public String reason;
        public Long revokedBy;

        public static RevokeRequest of(String subjectType, Long subjectId,
                                       String relation,
                                       String resourceType, Long resourceId,
                                       String reason) {
            return of(subjectType, subjectId, relation, resourceType, resourceId, reason, null);
        }

        public static RevokeRequest of(String subjectType, Long subjectId,
                                       String relation,
                                       String resourceType, Long resourceId,
                                       String reason, Long revokedBy) {
            RevokeRequest r = new RevokeRequest();
            r.subjectType = subjectType; r.subjectId = subjectId;
            r.relation = relation;
            r.resourceType = resourceType; r.resourceId = resourceId;
            r.reason = reason;
            r.revokedBy = revokedBy;
            return r;
        }
    }

    public record RelationAssignedEvent(Long relationId,
                                        String resourceType, Long resourceId,
                                        String relation,
                                        String subjectType, Long subjectId,
                                        Long grantedBy) {}

    public record RelationRevokedEvent(Long relationId,
                                       String resourceType, Long resourceId,
                                       String relation,
                                       String subjectType, Long subjectId,
                                       String reason, Long revokedBy) {}
}
