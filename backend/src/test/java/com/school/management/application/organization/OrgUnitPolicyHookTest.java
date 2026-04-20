package com.school.management.application.organization;

import com.school.management.application.organization.command.CreateOrgUnitCommand;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import com.school.management.domain.organization.service.OrgUnitDomainService;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.PolicyRegistry;
import com.school.management.infrastructure.extension.PolicyViolationException;
import com.school.management.infrastructure.extension.Violation;
import com.school.management.infrastructure.persistence.place.UniversalPlaceOccupantMapper;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * W1.5: 验证 OrgUnitApplicationService 生命周期方法在变更边界调用 PolicyRegistry.
 *
 * <p>契约:
 * <ul>
 *   <li>BEFORE_CREATE / BEFORE_UPDATE / BEFORE_DELETE: enforce() — BLOCK 违规抛
 *       PolicyViolationException, 后续业务逻辑不执行</li>
 *   <li>AFTER_CREATE / AFTER_UPDATE: check() — WARN 违规仅记录, 不抛异常, 不回滚</li>
 * </ul>
 *
 * <p>entityType 统一用 "org_unit" (与 entity_type_configs.entity_type 对齐).
 */
@ExtendWith(MockitoExtension.class)
class OrgUnitPolicyHookTest {

    @Mock OrgUnitRepository orgUnitRepository;
    @Mock OrgUnitTypeRepository orgUnitTypeRepository;
    @Mock OrgUnitDomainService orgUnitDomainService;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ActivityEventPublisher activityEventPublisher;
    @Mock AccessRelationRepository accessRelationRepository;
    @Mock UserDomainMapper userDomainMapper;
    @Mock UniversalPlaceOccupantMapper placeOccupantMapper;
    @Mock PolicyRegistry policyRegistry;

    @InjectMocks OrgUnitApplicationService service;

    // ======================= BEFORE_DELETE BLOCK =======================

    @Test
    void deleteOrgUnit_whenBeforePolicyBlocks_throwsAndDoesNotTouchBusinessLogic() {
        // 模拟插件 BLOCK 删除 (如 CLASS 还有归属学生).
        PolicyViolationException blocked = new PolicyViolationException(
                List.of(Violation.block("ORG_NOT_EMPTY",
                        "CLASS 删除前必须无归属学生")));
        when(policyRegistry.enforce(any(PolicyContext.class))).thenThrow(blocked);

        assertThatThrownBy(() -> service.deleteOrgUnit(42L))
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("ORG_NOT_EMPTY");

        // BLOCK 发生在业务前, 任何副作用都不应出现.
        verify(orgUnitRepository, never()).findById(any());
        verify(orgUnitRepository, never()).deleteById(any());
        verify(userDomainMapper, never()).clearPrimaryOrgUnitId(any());
        verify(accessRelationRepository, never()).deleteByResource(any(), any());
    }

    // ======================= BEFORE_CREATE BLOCK =======================

    @Test
    void createOrgUnit_whenBeforePolicyBlocks_throwsAndDoesNotTouchBusinessLogic() {
        // 模拟插件 BLOCK 创建 (如非法父子层级).
        PolicyViolationException blocked = new PolicyViolationException(
                List.of(Violation.block("INVALID_PARENT_TYPE",
                        "CLASS 不允许作为 SCHOOL 的直接子节点")));
        when(policyRegistry.enforce(any(PolicyContext.class))).thenThrow(blocked);

        CreateOrgUnitCommand cmd = CreateOrgUnitCommand.builder()
                .unitCode("CL001")
                .unitName("一年级1班")
                .unitType("CLASS")
                .parentId(1L)
                .createdBy(1L)
                .build();

        assertThatThrownBy(() -> service.createOrgUnit(cmd))
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("INVALID_PARENT_TYPE");

        // BLOCK 发生在业务前, 领域服务和仓储都不应被调用.
        verify(orgUnitDomainService, never()).createOrgUnit(
                any(), any(), any(), any(), any());
        verify(orgUnitRepository, never()).save(any());
        // AFTER check 也不应被调用 (BEFORE 抛出后流程中断).
        verify(policyRegistry, never()).check(any(PolicyContext.class));
    }

    // ======================= AFTER_CREATE WARN =======================

    @Test
    void createOrgUnit_whenAfterPolicyWarns_completesSuccessfullyAndLogsWarnings() {
        // BEFORE: 放行.
        when(policyRegistry.enforce(any(PolicyContext.class)))
                .thenReturn(Collections.emptyList());
        // AFTER: WARN.
        when(policyRegistry.check(any(PolicyContext.class)))
                .thenReturn(List.of(Violation.warn("DEEP_NESTING",
                        "tree_level 已超过推荐的 6 层")));

        // 业务依赖 stub:
        OrgUnit created = OrgUnit.builder()
                .id(500L)
                .unitCode("OU001")
                .unitName("研发部")
                .unitType("DEPARTMENT")
                .build();
        when(orgUnitDomainService.createOrgUnit(any(), any(), any(), any(), any()))
                .thenReturn(created);
        when(orgUnitRepository.save(any(OrgUnit.class))).thenReturn(created);
        when(orgUnitTypeRepository.findByTypeCode(any())).thenReturn(Optional.empty());

        CreateOrgUnitCommand cmd = CreateOrgUnitCommand.builder()
                .unitCode("OU001")
                .unitName("研发部")
                .unitType("DEPARTMENT")
                .parentId(1L)
                .createdBy(1L)
                .build();

        // Act — WARN 不应抛异常.
        service.createOrgUnit(cmd);

        // 断言两阶段都被调用, 且 phase / entityType 正确.
        ArgumentCaptor<PolicyContext> beforeCtx = ArgumentCaptor.forClass(PolicyContext.class);
        verify(policyRegistry).enforce(beforeCtx.capture());
        assertThat(beforeCtx.getValue().entityType()).isEqualTo("org_unit");
        assertThat(beforeCtx.getValue().phase()).isEqualTo("BEFORE_CREATE");

        ArgumentCaptor<PolicyContext> afterCtx = ArgumentCaptor.forClass(PolicyContext.class);
        verify(policyRegistry).check(afterCtx.capture());
        assertThat(afterCtx.getValue().entityType()).isEqualTo("org_unit");
        assertThat(afterCtx.getValue().phase()).isEqualTo("AFTER_CREATE");

        // 业务逻辑完成: 仓储保存被调用.
        verify(orgUnitRepository).save(any(OrgUnit.class));
    }
}
