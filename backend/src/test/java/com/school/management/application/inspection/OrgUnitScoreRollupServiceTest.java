package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.OrgUnitScore;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.OrgUnitScoreRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 组织树得分 roll-up 单测 — 钉死 <b>MEAN (均值) 不 SUM (求和)</b>.
 *
 * <p>核心证明: 同样"1 个差班", 小部门(5 子组织)掉到 84, 大部门(20 子组织)还有 88.5 ——
 * 用均值, 子组织数不会碾压. 求和会让 20 子组织部门总分一定碾压 5 子组织部门, 这正是
 * "5 个班 vs 20 个班"不公平的根因; 均值让数量自动抵消.
 */
@ExtendWith(MockitoExtension.class)
class OrgUnitScoreRollupServiceTest {

    private static final LocalDate CYCLE = LocalDate.of(2026, 5, 29);
    private static final Long PROJECT_ID = 1000L;

    @Mock private InspTaskRepository taskRepository;
    @Mock private InspSubmissionRepository submissionRepository;
    @Mock private OrgUnitRepository orgUnitRepository;
    @Mock private OrgUnitScoreRepository orgUnitScoreRepository;

    @InjectMocks private OrgUnitScoreRollupService service;

    /** 每个被计算的 orgUnit 注册到 findById; submission 攒到一个 task. */
    private final List<InspSubmission> allSubmissions = new ArrayList<>();

    @BeforeEach
    void setup() {
        // 一个 task 承载本周期全部 submission.
        InspTask task = InspTask.reconstruct(InspTask.builder().id(1L).taskDate(CYCLE));
        when(taskRepository.findByProjectIdAndTaskDate(PROJECT_ID, CYCLE)).thenReturn(List.of(task));
        when(submissionRepository.findByTaskId(1L)).thenReturn(allSubmissions);

        // OrgUnitScore upsert: 永远 miss → create 新的, save 原样返回.
        lenient().when(orgUnitScoreRepository.findByProjectIdAndOrgUnitIdAndCycleDate(
                anyLong(), anyLong(), any())).thenReturn(Optional.empty());
        lenient().when(orgUnitScoreRepository.save(any(OrgUnitScore.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /** 注册一个组织单元 (id→OrgUnit). */
    private void registerOrg(Long id, Long parentId, int treeLevel) {
        OrgUnit unit = OrgUnit.builder()
                .id(id).unitCode("U" + id).unitName("U" + id).unitType("DEPT")
                .parentId(parentId).treeLevel(treeLevel)
                .build();
        lenient().when(orgUnitRepository.findById(id)).thenReturn(Optional.of(unit));
    }

    /** 给某叶子组织挂一个 COMPLETED submission, finalScore=score. */
    private void addLeafSubmission(Long orgUnitId, double score) {
        allSubmissions.add(InspSubmission.reconstruct(InspSubmission.builder()
                .id((long) (allSubmissions.size() + 1))
                .taskId(1L)
                .targetType(TargetType.ORG)
                .targetId(orgUnitId)
                .orgUnitId(orgUnitId)
                .status(SubmissionStatus.COMPLETED)
                .finalScore(BigDecimal.valueOf(score))));
    }

    @Test
    void rollupUsesMeanNotSum_smallDeptNotCrushedByBigDept() {
        // 部门A (id=10, level 1) 有 5 个子组织 (level 2), 分数 [90,90,90,90,60].
        registerOrg(10L, null, 1);
        double[] deptAChildScores = {90, 90, 90, 90, 60};
        for (int i = 0; i < deptAChildScores.length; i++) {
            Long childId = 100L + i;
            registerOrg(childId, 10L, 2);
            addLeafSubmission(childId, deptAChildScores[i]);
        }

        // 部门B (id=20, level 1) 有 20 个子组织 (level 2), 分数 [90×19, 60×1].
        registerOrg(20L, null, 1);
        for (int i = 0; i < 20; i++) {
            Long childId = 200L + i;
            registerOrg(childId, 20L, 2);
            addLeafSubmission(childId, i < 19 ? 90 : 60);
        }

        List<OrgUnitScore> results = service.rollup(PROJECT_ID, CYCLE);

        OrgUnitScore deptA = findByOrg(results, 10L);
        OrgUnitScore deptB = findByOrg(results, 20L);

        // 部门A = (90*4 + 60) / 5 = 84.00
        assertThat(deptA.getScore()).isEqualByComparingTo(new BigDecimal("84.00"));
        assertThat(deptA.getChildCount()).isEqualTo(5);

        // 部门B = (90*19 + 60) / 20 = 88.50
        assertThat(deptB.getScore()).isEqualByComparingTo(new BigDecimal("88.50"));
        assertThat(deptB.getChildCount()).isEqualTo(20);

        // 关键: 同样 1 个差班, 小部门(5)掉到 84.00, 大部门(20)还有 88.50 —— 均值, 数量不碾压.
        assertThat(deptA.getScore()).isLessThan(deptB.getScore());
    }

    @Test
    void leafOrgScoreIsMeanOfItsDirectSubmissions() {
        // 一个叶子组织 (id=30) 直接挂 3 个 submission [80,90,100] → 叶子分 = 90.00.
        registerOrg(30L, null, 1);
        addLeafSubmission(30L, 80);
        addLeafSubmission(30L, 90);
        addLeafSubmission(30L, 100);

        List<OrgUnitScore> results = service.rollup(PROJECT_ID, CYCLE);

        OrgUnitScore leaf = findByOrg(results, 30L);
        assertThat(leaf.getScore()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(leaf.getSourceCount()).isEqualTo(3);
        assertThat(leaf.getChildCount()).isZero();
    }

    private OrgUnitScore findByOrg(List<OrgUnitScore> results, Long orgUnitId) {
        return results.stream()
                .filter(r -> orgUnitId.equals(r.getOrgUnitId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 orgUnit " + orgUnitId + " 的得分"));
    }
}
