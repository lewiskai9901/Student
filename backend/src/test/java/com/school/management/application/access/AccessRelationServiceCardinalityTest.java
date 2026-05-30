package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.exception.CardinalityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Task 1.2 — forceGrant 真正强制 cardinality 单元测试.
 *
 * <p>验证:
 * <ul>
 *   <li>member (maxPerSubject=1): 同一 user grant 到不同 org 抛 {@link CardinalityViolationException}</li>
 *   <li>同 tuple 重复 grant 幂等不抛 (走 findActiveByTuple 命中)</li>
 *   <li>admin (maxPerResource=1): 同组织第二个 admin 抛异常</li>
 *   <li>无 cardinality 约束的关系不受限</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccessRelationService cardinality 强制")
class AccessRelationServiceCardinalityTest {

    @Mock private AccessRelationRepository repo;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private AccessCheckCache checkCache;
    @Mock private MetadataSchemaValidator metadataSchemaValidator;
    @Mock private RelationApprovalService relationApprovalService;
    @Mock private RelationTypeRegistry relationTypeRegistry;

    private AccessRelationService service;

    @BeforeEach
    void setUp() {
        service = new AccessRelationService(
            repo, eventPublisher, new ObjectMapper(), List.of(),
            jdbcTemplate, checkCache, metadataSchemaValidator,
            relationApprovalService, relationTypeRegistry);

        // 默认: 关系已注册、不需审批、无 tuple 命中、insert 成功
        lenient().when(repo.isRelationRegistered(anyString(), anyString(), anyString())).thenReturn(true);
        lenient().when(relationApprovalService.requiresApproval(anyString())).thenReturn(false);
        lenient().when(repo.findActiveByTuple(anyString(), anyLong(), anyString(), anyString(), anyLong()))
            .thenReturn(Optional.empty());
        lenient().when(repo.insertDirect(any())).thenReturn(999L);
    }

    private AccessRelationService.GrantRequest memberReq(Long userId, Long orgId) {
        AccessRelationService.GrantRequest r = AccessRelationService.GrantRequest.of(
            "user", userId, "member", "org_unit", orgId);
        r.grantedBy = 1L;
        return r;
    }

    private AccessRelationService.GrantRequest adminReq(Long userId, Long orgId) {
        AccessRelationService.GrantRequest r = AccessRelationService.GrantRequest.of(
            "user", userId, "admin", "org_unit", orgId);
        r.grantedBy = 1L;
        return r;
    }

    @Test
    @DisplayName("member maxPerSubject=1: 第一次 grant 成功, 第二个不同 org 抛 CardinalityViolationException")
    void memberUniquePerSubject() {
        when(relationTypeRegistry.getCardinality("member", "user", "org_unit"))
            .thenReturn(new RelationTypeRegistry.Cardinality(1, null));

        // 第一次: 该 subject 当前 0 个 member 关系 → 通过
        when(repo.countActiveBySubjectRelation("user", 10L, "member")).thenReturn(0);
        Long id1 = service.grant(memberReq(10L, 100L));
        assertThat(id1).isEqualTo(999L);
        verify(repo).insertDirect(any());

        // 第二次: 该 subject 已有 1 个 member 关系 → 超限拒绝
        when(repo.countActiveBySubjectRelation("user", 10L, "member")).thenReturn(1);
        assertThatThrownBy(() -> service.grant(memberReq(10L, 200L)))
            .isInstanceOf(CardinalityViolationException.class)
            .hasMessageContaining("member")
            .hasMessageContaining("上限");
    }

    @Test
    @DisplayName("同 tuple 重复 grant 幂等不抛 (findActiveByTuple 命中, 不触发 cardinality 计数)")
    void sameTupleIsIdempotent() {
        // tuple 已存在 → 直接返回现有 id, 不进入 enforceCardinality
        when(repo.findActiveByTuple("org_unit", 100L, "member", "user", 10L))
            .thenReturn(Optional.of(555L));

        Long id = service.grant(memberReq(10L, 100L));

        assertThat(id).isEqualTo(555L);
        verify(repo, never()).insertDirect(any());
        verify(repo, never()).countActiveBySubjectRelation(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("admin maxPerResource=1: 同组织第二个 admin 抛 CardinalityViolationException")
    void adminUniquePerResource() {
        when(relationTypeRegistry.getCardinality("admin", "user", "org_unit"))
            .thenReturn(new RelationTypeRegistry.Cardinality(null, 1));

        // 第一个 admin: 该 org 当前 0 个 admin → 通过
        when(repo.countActiveByResourceRelation("org_unit", 100L, "admin")).thenReturn(0);
        Long id1 = service.grant(adminReq(10L, 100L));
        assertThat(id1).isEqualTo(999L);

        // 第二个 admin (不同 user, 同 org): 已有 1 个 → 超限拒绝
        when(repo.countActiveByResourceRelation("org_unit", 100L, "admin")).thenReturn(1);
        assertThatThrownBy(() -> service.grant(adminReq(20L, 100L)))
            .isInstanceOf(CardinalityViolationException.class)
            .hasMessageContaining("admin");
    }

    @Test
    @DisplayName("无 cardinality 约束的关系: 不计数、不受限")
    void unboundedRelationNotLimited() {
        when(relationTypeRegistry.getCardinality(eq("watches"), anyString(), anyString()))
            .thenReturn(new RelationTypeRegistry.Cardinality(null, null));

        AccessRelationService.GrantRequest r = AccessRelationService.GrantRequest.of(
            "user", 10L, "watches", "org_unit", 100L);
        r.grantedBy = 1L;

        Long id = service.grant(r);

        assertThat(id).isEqualTo(999L);
        verify(repo, never()).countActiveBySubjectRelation(anyString(), anyLong(), anyString());
        verify(repo, never()).countActiveByResourceRelation(anyString(), anyLong(), anyString());
    }
}
