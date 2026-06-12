package com.school.management.application.place;

import com.school.management.application.access.AccessRelationService;
import com.school.management.application.access.AccessRelationService.GrantRequest;
import com.school.management.application.access.AccessRelationService.RevokeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PlaceOrgResolver 单测 — 场所归属/责任人统一入口编排逻辑.
 *
 * 验证 (对称 MembershipResolverTest):
 *   - orgOf / responsibleOf 覆盖点直查 (不含继承)
 *   - setBelonging / setResponsible 必须 revoke-before-grant + 关系三元组方向正确
 *     (belongs_to: subject=place→resource=org_unit; responsible_for: subject=user→resource=place)
 *   - clearBelonging / clearResponsible 无覆盖点时 no-op (不误发 revoke)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceOrgResolver 场所归属统一入口")
class PlaceOrgResolverTest {

    @Mock AccessRelationService accessRelationService;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks PlaceOrgResolver resolver;

    // ──────────────────────────────────────────────
    // 查询
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("orgOf: 返回显式归属覆盖点")
    void orgOf_returnsOverride() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of(100L));

        assertThat(resolver.orgOf(5L)).contains(100L);
    }

    @Test
    @DisplayName("orgOf: 继承态场所 (无覆盖点) 返回空")
    void orgOf_inheritedPlaceIsEmpty() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of());

        assertThat(resolver.orgOf(5L)).isEmpty();
    }

    @Test
    @DisplayName("orgOf: 脏数据多行取首条")
    void orgOf_multipleRowsTakesFirst() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of(100L, 200L));

        assertThat(resolver.orgOf(5L)).contains(100L);
    }

    @Test
    @DisplayName("responsibleOf: 返回显式责任人覆盖点")
    void responsibleOf_returnsOverride() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of(7L));

        assertThat(resolver.responsibleOf(5L)).contains(7L);
    }

    @Test
    @DisplayName("overridesFor: 空集合短路, 不发 SQL")
    void overridesFor_emptyShortCircuit() {
        assertThat(resolver.overridesFor(List.of())).isEmpty();
        verify(jdbcTemplate, never()).query(anyString(),
            any(org.springframework.jdbc.core.RowCallbackHandler.class), any(Object[].class));
    }

    // ──────────────────────────────────────────────
    // setBelonging — grant-or-replace
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("setBelonging: revoke-before-grant, 三元组方向 place->org_unit")
    void setBelonging_revokeBeforeGrant() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of(100L)); // 现有覆盖点 org=100

        resolver.setBelonging(5L, 200L);

        InOrder order = inOrder(accessRelationService);
        ArgumentCaptor<RevokeRequest> revoke = ArgumentCaptor.forClass(RevokeRequest.class);
        ArgumentCaptor<GrantRequest> grant = ArgumentCaptor.forClass(GrantRequest.class);
        order.verify(accessRelationService).revoke(revoke.capture());
        order.verify(accessRelationService).forceGrant(grant.capture());

        assertThat(revoke.getValue().subjectType).isEqualTo("place");
        assertThat(revoke.getValue().subjectId).isEqualTo(5L);
        assertThat(revoke.getValue().relation).isEqualTo("belongs_to");
        assertThat(revoke.getValue().resourceId).isEqualTo(100L);

        assertThat(grant.getValue().subjectType).isEqualTo("place");
        assertThat(grant.getValue().subjectId).isEqualTo(5L);
        assertThat(grant.getValue().relation).isEqualTo("belongs_to");
        assertThat(grant.getValue().resourceType).isEqualTo("org_unit");
        assertThat(grant.getValue().resourceId).isEqualTo(200L);
    }

    @Test
    @DisplayName("setBelonging: 原本继承态 (无覆盖点) 直接 grant, 不发 revoke")
    void setBelonging_noExistingOverride() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of());

        resolver.setBelonging(5L, 200L);

        verify(accessRelationService, never()).revoke(any());
        verify(accessRelationService).forceGrant(any(GrantRequest.class));
    }

    @Test
    @DisplayName("setBelonging: 空参拒绝")
    void setBelonging_nullArgsRejected() {
        assertThatThrownBy(() -> resolver.setBelonging(null, 1L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resolver.setBelonging(1L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("clearBelonging: 无覆盖点 no-op")
    void clearBelonging_noopWhenInherited() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of());

        resolver.clearBelonging(5L);

        verify(accessRelationService, never()).revoke(any());
    }

    // ──────────────────────────────────────────────
    // setResponsible — 注意方向相反: subject=user, resource=place
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("setResponsible: 三元组方向 user->place")
    void setResponsible_direction() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of(7L)); // 现有责任人 user=7

        resolver.setResponsible(5L, 9L);

        InOrder order = inOrder(accessRelationService);
        ArgumentCaptor<RevokeRequest> revoke = ArgumentCaptor.forClass(RevokeRequest.class);
        ArgumentCaptor<GrantRequest> grant = ArgumentCaptor.forClass(GrantRequest.class);
        order.verify(accessRelationService).revoke(revoke.capture());
        order.verify(accessRelationService).forceGrant(grant.capture());

        assertThat(revoke.getValue().subjectType).isEqualTo("user");
        assertThat(revoke.getValue().subjectId).isEqualTo(7L);
        assertThat(revoke.getValue().resourceType).isEqualTo("place");
        assertThat(revoke.getValue().resourceId).isEqualTo(5L);

        assertThat(grant.getValue().subjectType).isEqualTo("user");
        assertThat(grant.getValue().subjectId).isEqualTo(9L);
        assertThat(grant.getValue().relation).isEqualTo("responsible_for");
        assertThat(grant.getValue().resourceType).isEqualTo("place");
        assertThat(grant.getValue().resourceId).isEqualTo(5L);
    }

    @Test
    @DisplayName("clearResponsible: 无覆盖点 no-op")
    void clearResponsible_noopWhenInherited() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(5L)))
            .thenReturn(List.of());

        resolver.clearResponsible(5L);

        verify(accessRelationService, never()).revoke(any());
    }
}
