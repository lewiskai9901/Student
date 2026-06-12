package com.school.management.application.place;

import com.school.management.application.access.AccessRelationService.RelationAssignedEvent;
import com.school.management.application.access.AccessRelationService.RelationRevokedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PlaceOrgProjector 单测 — effective_org_unit_id 投影重算逻辑.
 *
 * 验证:
 *   - 事件过滤: 仅 belongs_to|place 触发重算
 *   - 继承级联: 无覆盖点的子孙跟随子树根基准 (父 effective)
 *   - 子树截断: 显式覆盖的分支不被父变更影响, 其子孙跟随覆盖值
 *   - revoke 回落: 覆盖点撤销后整支回落父 effective
 *   - 差异写: 已一致的行不进 batchUpdate
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceOrgProjector 投影重算")
class PlaceOrgProjectorTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock PlaceOrgResolver placeOrgResolver;

    @InjectMocks PlaceOrgProjector projector;

    // ──────────────────────────────────────────────
    // 事件过滤
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("非 belongs_to 关系事件不触发重算")
    void ignoresOtherRelations() {
        projector.onRelationAssigned(new RelationAssignedEvent(
            1L, "org_unit", 100L, "member", "user", 7L, null));
        projector.onRelationRevoked(new RelationRevokedEvent(
            2L, "place", 5L, "responsible_for", "user", 7L, "x", null));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("belongs_to|place 事件触发 subject 子树重算")
    void belongsToTriggersRecompute() {
        when(jdbcTemplate.queryForMap(anyString(), eq(5L)))
            .thenThrow(new EmptyResultDataAccessException(1)); // 场所不存在 → no-op 即可证明已进入

        projector.onRelationAssigned(new RelationAssignedEvent(
            1L, "org_unit", 100L, "belongs_to", "place", 5L, null));

        verify(jdbcTemplate).queryForMap(anyString(), eq(5L));
    }

    // ──────────────────────────────────────────────
    // 重算语义
    // ──────────────────────────────────────────────

    /** 树: root(1,覆盖=100) → child(2,无覆盖) → grandchild(3,覆盖=300) → ggc(4,无覆盖) */
    private void stubTree(Map<Long, Long> overrides, Long parentBase,
                          Long... currentEffective) {
        Map<String, Object> root = row(1L, null, "/1/", currentEffective[0]);
        when(jdbcTemplate.queryForMap(anyString(), eq(1L))).thenReturn(root);
        lenient().when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(99L)))
            .thenReturn(parentBase);
        when(jdbcTemplate.queryForList(anyString(), eq("/1/")))
            .thenReturn(List.of(
                row(1L, null, "/1/", currentEffective[0]),
                row(2L, 1L, "/1/2/", currentEffective[1]),
                row(3L, 2L, "/1/2/3/", currentEffective[2]),
                row(4L, 3L, "/1/2/3/4/", currentEffective[3])));
        when(placeOrgResolver.overridesFor(any())).thenReturn(overrides);
    }

    private Map<String, Object> row(Long id, Long parentId, String path, Long effective) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id); m.put("parent_id", parentId); m.put("path", path);
        m.put("effective_org_unit_id", effective);
        return m;
    }

    @Test
    @DisplayName("继承级联 + 子树截断: 覆盖分支跟随自身覆盖值, 其余跟随根覆盖")
    void cascadeWithOverrideCutoff() {
        // root 覆盖=100, grandchild 覆盖=300; 当前列全为陈旧 NULL
        stubTree(Map.of(1L, 100L, 3L, 300L), null, null, null, null, null);

        projector.recomputeSubtree(1L);

        ArgumentCaptor<List<Object[]>> batch = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(contains("effective_org_unit_id"), batch.capture());
        Map<Long, Long> updated = new HashMap<>();
        batch.getValue().forEach(args -> updated.put((Long) args[1], (Long) args[0]));

        assertThat(updated).containsEntry(1L, 100L)  // 根 = 自身覆盖
            .containsEntry(2L, 100L)                 // 继承根
            .containsEntry(3L, 300L)                 // 截断: 自身覆盖
            .containsEntry(4L, 300L);                // 跟随覆盖分支
    }

    @Test
    @DisplayName("revoke 回落: 无任何覆盖点时整树回落 NULL (根无父)")
    void revokeFallsBackToBase() {
        stubTree(Map.of(), null, 100L, 100L, 100L, 100L); // 列上还是旧值 100

        projector.recomputeSubtree(1L);

        ArgumentCaptor<List<Object[]>> batch = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), batch.capture());
        assertThat(batch.getValue()).hasSize(4)
            .allSatisfy(args -> assertThat(args[0]).isNull());
    }

    @Test
    @DisplayName("差异写: 列值已一致时不发 batchUpdate")
    void noDiffNoWrite() {
        stubTree(Map.of(1L, 100L), null, 100L, 100L, 100L, 100L);

        projector.recomputeSubtree(1L);

        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(List.class));
    }

    @Test
    @DisplayName("子树根有父: 基准取父 effective (子树外列值即真)")
    void baseFromParentEffective() {
        // 场所 1 的父是 99 (effective=500); 1 无覆盖 → 整树回落 500
        Map<String, Object> root = row(1L, 99L, "/99/1/", null);
        when(jdbcTemplate.queryForMap(anyString(), eq(1L))).thenReturn(root);
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(99L)))
            .thenReturn(500L);
        when(jdbcTemplate.queryForList(anyString(), eq("/99/1/")))
            .thenReturn(List.of(row(1L, 99L, "/99/1/", null)));
        when(placeOrgResolver.overridesFor(any())).thenReturn(Map.of());

        projector.recomputeSubtree(1L);

        ArgumentCaptor<List<Object[]>> batch = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), batch.capture());
        assertThat(batch.getValue()).hasSize(1);
        assertThat(batch.getValue().get(0)[0]).isEqualTo(500L);
    }

    @Test
    @DisplayName("场所不存在/已删: no-op")
    void missingPlaceNoop() {
        when(jdbcTemplate.queryForMap(anyString(), eq(5L)))
            .thenThrow(new EmptyResultDataAccessException(1));

        projector.recomputeSubtree(5L);

        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(List.class));
    }
}
