package com.school.management.application.inspection;

import com.school.management.application.inspection.dto.OrgScoreView;
import com.school.management.domain.inspection.model.execution.OrgUnitScore;
import com.school.management.domain.inspection.repository.OrgUnitScoreRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 组织树得分查询服务单测 (Phase 3.5a) — 钉死: 按 score 降序 (排名) + 字段映射 + 组织名补齐.
 */
@ExtendWith(MockitoExtension.class)
class OrgUnitScoreQueryServiceTest {

    private static final Long PROJECT_ID = 1000L;
    private static final LocalDate CYCLE = LocalDate.of(2026, 5, 29);

    @Mock private OrgUnitScoreRepository orgUnitScoreRepository;
    @Mock private OrgUnitRepository orgUnitRepository;

    @InjectMocks private OrgUnitScoreQueryService service;

    private OrgUnitScore score(Long orgUnitId, double score, String grade, int childCount, int sourceCount) {
        return OrgUnitScore.reconstruct(OrgUnitScore.builder()
                .id(orgUnitId)
                .projectId(PROJECT_ID)
                .orgUnitId(orgUnitId)
                .cycleDate(CYCLE)
                .score(BigDecimal.valueOf(score))
                .grade(grade)
                .childCount(childCount)
                .sourceCount(sourceCount));
    }

    private OrgUnit org(Long id, String name) {
        return OrgUnit.builder()
                .id(id).unitCode("U" + id).unitName(name).unitType("DEPT")
                .build();
    }

    @Test
    void getOrgScores_sortedByScoreDescending_withNamesAndFields() {
        // 三个组织, 故意乱序返回, 分数 84 / 92 / 88.5.
        when(orgUnitScoreRepository.findByProjectIdAndCycleDate(eq(PROJECT_ID), eq(CYCLE)))
                .thenReturn(List.of(
                        score(10L, 84.00, "B", 5, 0),
                        score(20L, 92.00, "A", 3, 0),
                        score(30L, 88.50, "B", 0, 4)));
        when(orgUnitRepository.findByIds(any())).thenReturn(List.of(
                org(10L, "部门A"), org(20L, "部门B"), org(30L, "三班")));

        List<OrgScoreView> result = service.getOrgScores(PROJECT_ID, CYCLE);

        // 按 score 降序: 92 → 88.5 → 84.
        assertThat(result).hasSize(3);
        assertThat(result).extracting(OrgScoreView::getOrgUnitId)
                .containsExactly(20L, 30L, 10L);
        assertThat(result).extracting(OrgScoreView::getScore)
                .containsExactly(new BigDecimal("92.0"), new BigDecimal("88.5"), new BigDecimal("84.0"));

        // 第一名字段映射 + 组织名补齐.
        OrgScoreView top = result.get(0);
        assertThat(top.getOrgUnitId()).isEqualTo(20L);
        assertThat(top.getOrgUnitName()).isEqualTo("部门B");
        assertThat(top.getGrade()).isEqualTo("A");
        assertThat(top.getChildCount()).isEqualTo(3);
        assertThat(top.getSourceCount()).isZero();

        // 叶子组织 sourceCount 透出.
        OrgScoreView leaf = result.stream()
                .filter(v -> v.getOrgUnitId().equals(30L)).findFirst().orElseThrow();
        assertThat(leaf.getOrgUnitName()).isEqualTo("三班");
        assertThat(leaf.getSourceCount()).isEqualTo(4);
        assertThat(leaf.getChildCount()).isZero();
    }

    @Test
    void getOrgScores_emptyWhenNoRollupRows() {
        when(orgUnitScoreRepository.findByProjectIdAndCycleDate(eq(PROJECT_ID), eq(CYCLE)))
                .thenReturn(List.of());

        List<OrgScoreView> result = service.getOrgScores(PROJECT_ID, CYCLE);

        assertThat(result).isEmpty();
    }
}
