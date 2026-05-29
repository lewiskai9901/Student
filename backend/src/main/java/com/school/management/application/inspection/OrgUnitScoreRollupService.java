package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.OrgUnitScore;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.OrgUnitScoreRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组织树得分 roll-up 服务 (规模公平性 Stage 5)
 *
 * <p>对一个 (project, cycleDate) 的所有已完成 submission, 按其 target 所属 orgUnit 分组,
 * 沿组织树自底向上用 <b>MEAN (均值, 不是 SUM)</b> 逐层滚动:
 * <ul>
 *   <li>叶子组织分 = 该组织下 submission.finalScore 的均值 (sourceCount = submission 数)</li>
 *   <li>父组织分   = 直接子组织 (已算分者) 分数的均值     (childCount  = 子组织数)</li>
 * </ul>
 *
 * <p><b>为什么用 MEAN 而不是 SUM</b>: 求和会让子组织多的部门吃亏 (20 个班一定比 5 个班
 * 总分高), 均值让数量自动抵消 —— "1 个差班"对小部门(5)和大部门(20)的影响不同, 大部门被
 * 更多好班稀释, 这正是规模公平的核心.
 *
 * <p><b>分组依据</b>: 用 {@link InspSubmission#getOrgUnitId()} —— 当 targetType=ORG 时它就是
 * targetId; USER/PLACE/ASSET 类型由 application service 经 lookup 注入 (见 InspSubmission.create).
 * orgUnitId 为 null 的 submission (未关联组织) 本期跳过, 不参与 roll-up.
 *
 * <p><b>不接线触发</b>: 本服务只提供 rollup 方法, 不在 ScoreAggregationService 内自动调用 ——
 * 触发接线是后续 Phase 3.4 的工作.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OrgUnitScoreRollupService {

    private static final int SCALE = 2;

    private final InspTaskRepository taskRepository;
    private final InspSubmissionRepository submissionRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgUnitScoreRepository orgUnitScoreRepository;

    /**
     * 对 (project, cycleDate) 执行组织树得分 roll-up.
     *
     * @return 写入/更新的 OrgUnitScore 列表 (按计算顺序: 最深先算)
     */
    @Transactional
    public List<OrgUnitScore> rollup(Long projectId, LocalDate cycleDate) {
        // 1. 取该 (project, cycleDate) 所有 task → submission, 过滤已完成且有分有组织.
        //    按 orgUnitId 收集叶子层 submission 分数.
        Map<Long, List<BigDecimal>> leafScoresByOrg = new HashMap<>();
        List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDate(projectId, cycleDate);
        for (InspTask task : tasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
            for (InspSubmission sub : submissions) {
                if (sub.getStatus() != SubmissionStatus.COMPLETED) continue;
                if (sub.getFinalScore() == null) continue;
                Long orgUnitId = sub.getOrgUnitId();
                if (orgUnitId == null) {
                    // 非 ORG target 未注入 orgUnitId (USER/PLACE/ASSET 无组织归属) — 本期跳过.
                    continue;
                }
                leafScoresByOrg.computeIfAbsent(orgUnitId, k -> new ArrayList<>())
                        .add(sub.getFinalScore());
            }
        }

        if (leafScoresByOrg.isEmpty()) {
            log.debug("项目 {} 日期 {} 无可 roll-up 的已完成提交, 跳过组织树汇总", projectId, cycleDate);
            return List.of();
        }

        // 2. 收集涉及的全部 orgUnit: 有直接 submission 的 orgUnit + 它们沿 parentId 链向上的祖先.
        //    祖先即使本身没有直接 submission, 也要算 (它的分 = 子组织分的均值).
        Map<Long, OrgUnit> orgUnits = collectOrgUnitsWithAncestors(leafScoresByOrg.keySet());

        // 3. 按 treeLevel 深度降序排 (最深先算), 保证算父之前子已就绪.
        List<OrgUnit> ordered = new ArrayList<>(orgUnits.values());
        ordered.sort(Comparator.comparingInt(
                (OrgUnit u) -> u.getTreeLevel() != null ? u.getTreeLevel() : 0).reversed());

        // 4. 逐个 roll-up.
        Map<Long, BigDecimal> computedScore = new HashMap<>();
        List<OrgUnitScore> results = new ArrayList<>();
        for (OrgUnit unit : ordered) {
            Long unitId = unit.getId();

            // 直接子组织里"已算出分数"的 (只有在本次计算集合内的才算).
            List<BigDecimal> childScores = new ArrayList<>();
            for (OrgUnit candidate : orgUnits.values()) {
                if (unitId.equals(candidate.getParentId()) && computedScore.containsKey(candidate.getId())) {
                    childScores.add(computedScore.get(candidate.getId()));
                }
            }

            BigDecimal score;
            int childCount;
            int sourceCount;
            if (!childScores.isEmpty()) {
                // 父组织: 取直接子组织分的均值.
                score = mean(childScores);
                childCount = childScores.size();
                sourceCount = 0;
            } else {
                // 叶子组织: 取组内 submission.finalScore 的均值.
                List<BigDecimal> leaf = leafScoresByOrg.get(unitId);
                if (leaf == null || leaf.isEmpty()) {
                    // 既无已算子组织, 也无叶子 submission — 该祖先此周期无数据, 跳过.
                    continue;
                }
                score = mean(leaf);
                childCount = 0;
                sourceCount = leaf.size();
            }

            computedScore.put(unitId, score);
            results.add(upsert(projectId, unitId, cycleDate, score, childCount, sourceCount));
        }

        log.info("组织树 roll-up 完成: projectId={}, date={}, 写入 {} 个组织得分",
                projectId, cycleDate, results.size());
        return results;
    }

    /**
     * 从叶子 orgUnit 集合出发, 沿 parentId 链向上收集所有祖先, 返回 id→OrgUnit 映射.
     */
    private Map<Long, OrgUnit> collectOrgUnitsWithAncestors(Set<Long> leafOrgIds) {
        Map<Long, OrgUnit> result = new HashMap<>();
        Set<Long> toResolve = new LinkedHashSet<>(leafOrgIds);
        while (!toResolve.isEmpty()) {
            Set<Long> next = new LinkedHashSet<>();
            for (Long id : toResolve) {
                if (id == null || result.containsKey(id)) continue;
                OrgUnit unit = orgUnitRepository.findById(id).orElse(null);
                if (unit == null) {
                    log.warn("roll-up 引用的组织单元不存在: orgUnitId={}", id);
                    continue;
                }
                result.put(id, unit);
                if (unit.getParentId() != null && !result.containsKey(unit.getParentId())) {
                    next.add(unit.getParentId());
                }
            }
            toResolve = next;
        }
        return result;
    }

    /** 均值, 2 位精度 HALF_UP. */
    private BigDecimal mean(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            sum = sum.add(v);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * upsert 进 org_unit_scores (唯一键 tenant_id+project_id+org_unit_id+cycle_date 幂等).
     * 并发冲突 catch DuplicateKeyException — 胜出线程已基于相同数据写入等价汇总.
     */
    private OrgUnitScore upsert(Long projectId, Long orgUnitId, LocalDate cycleDate,
                                BigDecimal score, int childCount, int sourceCount) {
        try {
            OrgUnitScore os = orgUnitScoreRepository
                    .findByProjectIdAndOrgUnitIdAndCycleDate(projectId, orgUnitId, cycleDate)
                    .orElse(OrgUnitScore.create(projectId, orgUnitId, cycleDate));
            os.updateScore(score, os.getGrade(), childCount, sourceCount);
            return orgUnitScoreRepository.save(os);
        } catch (DuplicateKeyException dup) {
            log.warn("OrgUnitScore 并发插入冲突 (唯一索引拦截), 本次放弃: project={}, orgUnit={}, date={}",
                    projectId, orgUnitId, cycleDate);
            return null;
        }
    }
}
