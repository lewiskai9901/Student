package com.school.management.application.organization;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.model.Position;
import com.school.management.domain.organization.model.entity.UserPosition;
import com.school.management.domain.organization.repository.PositionRepository;
import com.school.management.domain.organization.repository.UserPositionRepository;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 岗位变动 → access_relations + user.primaryOrgUnitId 自动同步
 *
 * - 岗位任命 → 创建 relation="staff" 的 access_relation（数据权限用）
 * - 主岗任命 → 同步更新 user.primaryOrgUnitId（统计归属用）
 * - 离任 → 清理 access_relation + primaryOrgUnitId
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PositionAccessSyncService {

    private final AccessRelationRepository accessRelationRepository;
    private final PositionRepository positionRepository;
    private final UserPositionRepository userPositionRepository;
    private final UserRepository userRepository;

    /**
     * 任命后同步
     */
    public void onAppoint(UserPosition up) {
        Long orgUnitId = resolveOrgUnitId(up.getPositionId());
        if (orgUnitId == null) return;

        // 1. 确保 user → org_unit 有 staff access_relation（数据权限）
        boolean exists = accessRelationRepository.exists(
            "org_unit", orgUnitId, "staff", "user", up.getUserId());
        if (!exists) {
            AccessRelation ar = AccessRelation.builder()
                .resourceType("org_unit")
                .resourceId(orgUnitId)
                .relation("staff")
                .subjectType("user")
                .subjectId(up.getUserId())
                .includeChildren(false)
                .accessLevel(1)
                .createdBy(up.getCreatedBy())
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
            accessRelationRepository.save(ar);
            log.info("Synced access_relation: user {} → org_unit {} (staff) on appoint", up.getUserId(), orgUnitId);
        }

        // 2. 如果是主岗且用户尚无归属组织 → 设置 user.primaryOrgUnitId
        //    注意：不覆盖已有的归属组织，归属组织通过 addMemberToOrg/removeMemberFromOrg 显式管理
        if (up.isPrimary()) {
            userRepository.findById(up.getUserId()).ifPresent(user -> {
                if (user.getPrimaryOrgUnitId() == null) {
                    user.setPrimaryOrgUnitId(orgUnitId);
                    userRepository.save(user);
                    log.info("Set user {} primaryOrgUnitId → {} (was null)", up.getUserId(), orgUnitId);
                } else {
                    log.info("Skipped updating user {} primaryOrgUnitId (already set to {})", up.getUserId(), user.getPrimaryOrgUnitId());
                }
            });
        }
    }

    /**
     * 离任后同步
     */
    public void onEndAppointment(UserPosition up) {
        Long orgUnitId = resolveOrgUnitId(up.getPositionId());
        if (orgUnitId == null) return;

        // 1. 检查用户在该组织是否还有其他活跃岗位
        List<UserPosition> remaining = userPositionRepository.findCurrentByOrgUnitId(orgUnitId);
        boolean stillHasPosition = remaining.stream()
            .anyMatch(r -> r.getUserId().equals(up.getUserId()));

        if (!stillHasPosition) {
            // 清理 staff access_relation
            List<AccessRelation> rels = accessRelationRepository.findByResource("org_unit", orgUnitId);
            rels.stream()
                .filter(r -> "user".equals(r.getSubjectType())
                    && r.getSubjectId().equals(up.getUserId())
                    && "staff".equals(r.getRelation()))
                .forEach(r -> {
                    accessRelationRepository.deleteById(r.getId());
                    log.info("Removed access_relation: user {} → org_unit {} (staff) on end appointment",
                        up.getUserId(), orgUnitId);
                });
        }

        // 2. 离任不影响 primaryOrgUnitId（归属组织通过 addMemberToOrg/removeMemberFromOrg 显式管理）
        //    只记录日志
        if (up.isPrimary()) {
            log.info("Primary position ended for user {} - primaryOrgUnitId unchanged (managed separately)", up.getUserId());
        }
    }

    /**
     * 调岗同步
     */
    public void onTransfer(UserPosition oldUp, UserPosition newUp) {
        onEndAppointment(oldUp);
        onAppoint(newUp);
    }

    private Long resolveOrgUnitId(Long positionId) {
        return positionRepository.findById(positionId)
            .map(Position::getOrgUnitId)
            .orElse(null);
    }
}
