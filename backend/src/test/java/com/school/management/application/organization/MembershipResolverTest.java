package com.school.management.application.organization;

import com.school.management.application.access.AccessRelationService;
import com.school.management.application.access.AccessRelationService.GrantRequest;
import com.school.management.application.access.AccessRelationService.RevokeRequest;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MembershipResolver 单测 — 归属统一查询/写入入口编排逻辑.
 *
 * 用 Mockito 隔离 AccessRelationService / AccessRelationRepository / JdbcTemplate /
 * OrgUnitRepository, 验证:
 *   - orgOf / membersOf / countMembers 直查转发
 *   - membersOfSubtree 真展开子树 (父+子 org 成员去重 union)
 *   - countMembersByType JOIN users 分组组装
 *   - setMembership 必须 revoke-before-grant + 新 member 行 isPrimary=1
 *   - clearMembership 转发 revoke
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipResolver 归属统一入口")
class MembershipResolverTest {

    @Mock AccessRelationService accessRelationService;
    @Mock AccessRelationRepository accessRelationRepository;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock OrgUnitRepository orgUnitRepository;

    @InjectMocks MembershipResolver resolver;

    // ──────────────────────────────────────────────
    // orgOf
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("orgOf: 反查用户唯一归属 org")
    void orgOf_returnsUniqueOrg() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L)))
            .thenReturn(List.of(100L));

        Optional<Long> org = resolver.orgOf(7L);

        assertThat(org).contains(100L);
    }

    @Test
    @DisplayName("orgOf: 无归属返回空")
    void orgOf_empty() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L)))
            .thenReturn(List.of());

        assertThat(resolver.orgOf(7L)).isEmpty();
    }

    // ──────────────────────────────────────────────
    // membersOf / countMembers
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("membersOf: 转发 findActiveSubjectIds(org_unit,member,user)")
    void membersOf_delegates() {
        when(accessRelationRepository.findActiveSubjectIds("org_unit", 100L, "member", "user"))
            .thenReturn(List.of(7L, 8L));

        List<Long> members = resolver.membersOf(100L);

        assertThat(members).containsExactly(7L, 8L);
    }

    @Test
    @DisplayName("countMembers: 直接成员数")
    void countMembers_size() {
        when(accessRelationRepository.findActiveSubjectIds("org_unit", 100L, "member", "user"))
            .thenReturn(List.of(7L, 8L, 9L));

        assertThat(resolver.countMembers(100L)).isEqualTo(3L);
    }

    // ──────────────────────────────────────────────
    // membersOfSubtree
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("membersOfSubtree: 父 org 返回包含子 org 成员, 去重")
    void membersOfSubtree_expandsChildren() {
        // 父 org=100 (treePath "/100/"), 子 org=101
        OrgUnit parent = OrgUnit.builder().id(100L).unitCode("P").unitName("Parent")
            .unitType("DEPARTMENT").treePath("/100/").build();
        OrgUnit child = OrgUnit.builder().id(101L).unitCode("C").unitName("Child")
            .unitType("DEPARTMENT").treePath("/100/101/").build();
        when(orgUnitRepository.findById(100L)).thenReturn(Optional.of(parent));
        when(orgUnitRepository.findDescendants("/100/")).thenReturn(List.of(child));

        // 父挂 7, 子挂 8 (7 同时也直挂父, 验证去重)
        when(accessRelationRepository.findActiveSubjectIds("org_unit", 100L, "member", "user"))
            .thenReturn(List.of(7L));
        when(accessRelationRepository.findActiveSubjectIds("org_unit", 101L, "member", "user"))
            .thenReturn(List.of(8L, 7L));

        List<Long> members = resolver.membersOfSubtree(100L);

        assertThat(members).containsExactlyInAnyOrder(7L, 8L);
    }

    @Test
    @DisplayName("membersOfSubtree: org 不存在返回空")
    void membersOfSubtree_missingOrg() {
        when(orgUnitRepository.findById(999L)).thenReturn(Optional.empty());
        assertThat(resolver.membersOfSubtree(999L)).isEmpty();
    }

    // ──────────────────────────────────────────────
    // countMembersByType
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("countMembersByType: JOIN users 按 user_type_code 分组")
    void countMembersByType_groups() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(100L)))
            .thenReturn(List.of(
                Map.entry("TEACHER", 3L),
                Map.entry("STUDENT", 28L)));

        Map<String, Long> counts = resolver.countMembersByType(100L);

        assertThat(counts).containsEntry("TEACHER", 3L).containsEntry("STUDENT", 28L);
    }

    // ──────────────────────────────────────────────
    // setMembership — revoke-before-grant + isPrimary=1
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("setMembership: 必须先 revoke 旧归属再 grant 新归属")
    void setMembership_revokeBeforeGrant() {
        // 现有归属 org=100
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L)))
            .thenReturn(List.of(100L));

        resolver.setMembership(7L, 200L);

        InOrder order = inOrder(accessRelationService);
        order.verify(accessRelationService).revoke(any(RevokeRequest.class));
        order.verify(accessRelationService).forceGrant(any(GrantRequest.class));
    }

    @Test
    @DisplayName("setMembership: 新 member 行 isPrimary=true")
    void setMembership_grantsPrimary() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L)))
            .thenReturn(List.of());

        resolver.setMembership(7L, 200L);

        ArgumentCaptor<GrantRequest> captor = ArgumentCaptor.forClass(GrantRequest.class);
        verify(accessRelationService).forceGrant(captor.capture());
        GrantRequest granted = captor.getValue();
        assertThat(granted.subjectType).isEqualTo("user");
        assertThat(granted.subjectId).isEqualTo(7L);
        assertThat(granted.relation).isEqualTo("member");
        assertThat(granted.resourceType).isEqualTo("org_unit");
        assertThat(granted.resourceId).isEqualTo(200L);
        assertThat(granted.isPrimary).isTrue();
    }

    @Test
    @DisplayName("setMembership: 无现有归属时不调 revoke (no-op clear)")
    void setMembership_noExistingNoRevoke() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L)))
            .thenReturn(List.of());

        resolver.setMembership(7L, 200L);

        verify(accessRelationService, org.mockito.Mockito.never()).revoke(any());
    }

    // ──────────────────────────────────────────────
    // clearMembership
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("clearMembership: 撤销现有 member 归属")
    void clearMembership_revokes() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L)))
            .thenReturn(List.of(100L));

        resolver.clearMembership(7L);

        ArgumentCaptor<RevokeRequest> captor = ArgumentCaptor.forClass(RevokeRequest.class);
        verify(accessRelationService).revoke(captor.capture());
        RevokeRequest revoked = captor.getValue();
        assertThat(revoked.subjectType).isEqualTo("user");
        assertThat(revoked.subjectId).isEqualTo(7L);
        assertThat(revoked.relation).isEqualTo("member");
        assertThat(revoked.resourceType).isEqualTo("org_unit");
        assertThat(revoked.resourceId).isEqualTo(100L);
    }

    @Test
    @DisplayName("clearMembership: 无归属时 no-op")
    void clearMembership_noop() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L)))
            .thenReturn(List.of());

        resolver.clearMembership(7L);

        verify(accessRelationService, org.mockito.Mockito.never()).revoke(any());
    }
}
