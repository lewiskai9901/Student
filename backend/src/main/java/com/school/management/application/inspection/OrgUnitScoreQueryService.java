package com.school.management.application.inspection;

import com.school.management.application.inspection.dto.OrgScoreView;
import com.school.management.domain.inspection.model.execution.OrgUnitScore;
import com.school.management.domain.inspection.repository.OrgUnitScoreRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组织树得分查询服务 (Phase 3.5a).
 *
 * <p>读路径: 把 {@link OrgUnitScoreRollupService} 写入 {@code org_unit_scores} 的 roll-up 结果
 * 暴露给前端, 补上组织名, 按 score 降序 (排名) 返回. 与写入服务分开, 职责清晰.
 *
 * <p>数据权限: 读 OrgUnitScore 经 mapper 层 {@code @DataPermission} + Filler 注入的 org_unit_id
 * 过滤 (检查平台横切关注点架构), 用户只能看到自己范围内的组织得分.
 */
@RequiredArgsConstructor
@Service
public class OrgUnitScoreQueryService {

    private final OrgUnitScoreRepository orgUnitScoreRepository;
    private final OrgUnitRepository orgUnitRepository;

    /**
     * 查 (project, cycleDate) 下各组织的 roll-up 得分, 补组织名, 按 score 降序.
     *
     * @return 组织得分视图列表 (score 降序; score 为 null 的排最后)
     */
    @Transactional(readOnly = true)
    public List<OrgScoreView> getOrgScores(Long projectId, LocalDate cycleDate) {
        List<OrgUnitScore> scores = orgUnitScoreRepository
                .findByProjectIdAndCycleDate(projectId, cycleDate);
        if (scores.isEmpty()) {
            return List.of();
        }

        // 批量取组织名 (一次查询, 避免 N+1).
        List<Long> orgIds = scores.stream()
                .map(OrgUnitScore::getOrgUnitId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> nameById = orgUnitRepository.findByIds(orgIds).stream()
                .collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getUnitName));

        return scores.stream()
                .map(s -> new OrgScoreView(
                        s.getOrgUnitId(),
                        nameById.get(s.getOrgUnitId()),
                        s.getScore(),
                        s.getGrade(),
                        s.getChildCount(),
                        s.getSourceCount()))
                // score 降序 (排名); null 分排最后.
                .sorted(Comparator.comparing(OrgScoreView::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
