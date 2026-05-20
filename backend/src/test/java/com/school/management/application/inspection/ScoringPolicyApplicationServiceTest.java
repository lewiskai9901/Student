package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.scoring.PolicyCalcRule;
import com.school.management.domain.inspection.model.scoring.PolicyGradeBand;
import com.school.management.domain.inspection.model.scoring.ScoringPolicy;
import com.school.management.domain.inspection.repository.PolicyCalcRuleRepository;
import com.school.management.domain.inspection.repository.PolicyGradeBandRepository;
import com.school.management.domain.inspection.repository.ScoringPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScoringPolicyApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 3 个 repository, 验证评分方案 / 等级段 / 计算规则的 CRUD 编排逻辑.
 * 注意: SecurityUtils.getCurrentUserId() 在无 SecurityContext 下返回 null — 测试容忍.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringPolicyApplicationService 评分方案应用服务")
class ScoringPolicyApplicationServiceTest {

    @Mock ScoringPolicyRepository policyRepository;
    @Mock PolicyGradeBandRepository gradeBandRepository;
    @Mock PolicyCalcRuleRepository calcRuleRepository;

    @InjectMocks ScoringPolicyApplicationService service;

    // -------- fixtures --------

    private ScoringPolicy policy(Long id, boolean isSystem) {
        return ScoringPolicy.reconstruct(ScoringPolicy.builder()
                .id(id).policyCode("PC-1").policyName("方案A").isSystem(isSystem));
    }

    private PolicyGradeBand band(Long id, Long policyId, String code) {
        return PolicyGradeBand.reconstruct(PolicyGradeBand.builder()
                .id(id).policyId(policyId).gradeCode(code).gradeName(code + "名")
                .minPercent(new BigDecimal("60")).maxPercent(new BigDecimal("80"))
                .sortOrder(1));
    }

    private PolicyCalcRule rule(Long id, Long policyId, String code) {
        return PolicyCalcRule.reconstruct(PolicyCalcRule.builder()
                .id(id).policyId(policyId).ruleCode(code).ruleName(code + "名")
                .ruleType("PENALTY").priority(1).config("{}"));
    }

    // ============================================================
    @Nested
    @DisplayName("ScoringPolicy CRUD")
    class PolicyTests {

        @Test
        @DisplayName("listPolicies 透传 findAll")
        void shouldListPolicies() {
            ScoringPolicy p = policy(1L, false);
            when(policyRepository.findAll()).thenReturn(List.of(p));
            assertThat(service.listPolicies()).containsExactly(p);
        }

        @Test
        @DisplayName("getPolicy 透传 findById")
        void shouldGetPolicy() {
            ScoringPolicy p = policy(1L, false);
            when(policyRepository.findById(1L)).thenReturn(Optional.of(p));
            assertThat(service.getPolicy(1L)).containsSame(p);
        }

        @Test
        @DisplayName("createPolicy 用入参构建并保存")
        void shouldCreatePolicy() {
            ArgumentCaptor<ScoringPolicy> captor = ArgumentCaptor.forClass(ScoringPolicy.class);
            when(policyRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ScoringPolicy saved = service.createPolicy("PC-9", "新方案", "描述", 3, 5);

            assertThat(saved.getPolicyCode()).isEqualTo("PC-9");
            assertThat(saved.getPolicyName()).isEqualTo("新方案");
            assertThat(captor.getValue().getDescription()).isEqualTo("描述");
            assertThat(captor.getValue().getPrecisionDigits()).isEqualTo(3);
            assertThat(captor.getValue().getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("updatePolicy: 普通方案更新成功")
        void shouldUpdatePolicy() {
            ScoringPolicy p = policy(1L, false);
            when(policyRepository.findById(1L)).thenReturn(Optional.of(p));
            when(policyRepository.save(any(ScoringPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

            ScoringPolicy saved = service.updatePolicy(1L, "改名", "新描述", 4, 9);

            assertThat(saved.getPolicyName()).isEqualTo("改名");
            assertThat(saved.getDescription()).isEqualTo("新描述");
            assertThat(saved.getPrecisionDigits()).isEqualTo(4);
        }

        @Test
        @DisplayName("updatePolicy: 不存在 → IllegalArgumentException")
        void shouldRejectUpdateMissing() {
            when(policyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updatePolicy(99L, "x", "y", 2, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("评分方案不存在");
        }

        @Test
        @DisplayName("updatePolicy: 系统预置方案 → 聚合根抛 IllegalStateException")
        void shouldRejectUpdateSystemPolicy() {
            ScoringPolicy p = policy(1L, true);
            when(policyRepository.findById(1L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> service.updatePolicy(1L, "x", "y", 2, 0))
                    .isInstanceOf(IllegalStateException.class);
            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("deletePolicy: 级联删除 calcRule / gradeBand / policy")
        void shouldCascadeDeletePolicy() {
            ScoringPolicy p = policy(1L, false);
            when(policyRepository.findById(1L)).thenReturn(Optional.of(p));

            service.deletePolicy(1L);

            verify(calcRuleRepository).deleteByPolicyId(1L);
            verify(gradeBandRepository).deleteByPolicyId(1L);
            verify(policyRepository).deleteById(1L);
        }

        @Test
        @DisplayName("deletePolicy: 不存在 → IllegalArgumentException")
        void shouldRejectDeleteMissing() {
            when(policyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deletePolicy(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deletePolicy: 系统预置方案 → IllegalStateException, 不删除")
        void shouldRejectDeleteSystemPolicy() {
            ScoringPolicy p = policy(1L, true);
            when(policyRepository.findById(1L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> service.deletePolicy(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("系统预置");
            verify(policyRepository, never()).deleteById(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("PolicyGradeBand CRUD")
    class GradeBandTests {

        @Test
        @DisplayName("listGradeBands 透传 findByPolicyId")
        void shouldListGradeBands() {
            PolicyGradeBand b = band(1L, 10L, "A");
            when(gradeBandRepository.findByPolicyId(10L)).thenReturn(List.of(b));
            assertThat(service.listGradeBands(10L)).containsExactly(b);
        }

        @Test
        @DisplayName("createGradeBand: 校验方案存在后构建并保存")
        void shouldCreateGradeBand() {
            when(policyRepository.findById(10L)).thenReturn(Optional.of(policy(10L, false)));
            ArgumentCaptor<PolicyGradeBand> captor = ArgumentCaptor.forClass(PolicyGradeBand.class);
            when(gradeBandRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            PolicyGradeBand saved = service.createGradeBand(10L, "A", "优秀",
                    new BigDecimal("90"), new BigDecimal("100"), 1);

            assertThat(saved.getGradeCode()).isEqualTo("A");
            assertThat(captor.getValue().getPolicyId()).isEqualTo(10L);
            assertThat(captor.getValue().getMinPercent()).isEqualByComparingTo("90");
            assertThat(captor.getValue().getMaxPercent()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("createGradeBand: 方案不存在 → IllegalArgumentException")
        void shouldRejectCreateBandMissingPolicy() {
            when(policyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createGradeBand(99L, "A", "优秀",
                    BigDecimal.ZERO, BigDecimal.TEN, 1))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(gradeBandRepository, never()).save(any());
        }

        @Test
        @DisplayName("saveGradeBands: 先全删再批量重建")
        void shouldSaveGradeBands() {
            when(policyRepository.findById(10L)).thenReturn(Optional.of(policy(10L, false)));
            when(gradeBandRepository.save(any(PolicyGradeBand.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            List<PolicyGradeBand> input = List.of(band(1L, 10L, "A"), band(2L, 10L, "B"));

            List<PolicyGradeBand> result = service.saveGradeBands(10L, input);

            assertThat(result).hasSize(2);
            verify(gradeBandRepository).deleteByPolicyId(10L);
            verify(gradeBandRepository, times(2)).save(any(PolicyGradeBand.class));
        }

        @Test
        @DisplayName("saveGradeBands: 方案不存在 → IllegalArgumentException, 不删除")
        void shouldRejectSaveBandsMissingPolicy() {
            when(policyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.saveGradeBands(99L, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(gradeBandRepository, never()).deleteByPolicyId(any());
        }

        @Test
        @DisplayName("updateGradeBand: 找到对应段并重建保存")
        void shouldUpdateGradeBand() {
            when(gradeBandRepository.findByPolicyId(10L))
                    .thenReturn(List.of(band(1L, 10L, "A"), band(2L, 10L, "B")));
            ArgumentCaptor<PolicyGradeBand> captor = ArgumentCaptor.forClass(PolicyGradeBand.class);
            when(gradeBandRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            PolicyGradeBand saved = service.updateGradeBand(10L, 2L, "B2", "良好",
                    new BigDecimal("70"), new BigDecimal("85"), 3);

            assertThat(saved.getId()).isEqualTo(2L);
            assertThat(captor.getValue().getGradeCode()).isEqualTo("B2");
            assertThat(captor.getValue().getGradeName()).isEqualTo("良好");
            assertThat(captor.getValue().getSortOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("updateGradeBand: bandId 不存在 → IllegalArgumentException")
        void shouldRejectUpdateBandMissing() {
            when(gradeBandRepository.findByPolicyId(10L))
                    .thenReturn(List.of(band(1L, 10L, "A")));
            assertThatThrownBy(() -> service.updateGradeBand(10L, 999L, "X", "x",
                    BigDecimal.ZERO, BigDecimal.TEN, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("等级段不存在");
        }

        @Test
        @DisplayName("deleteGradeBand: 全删后重写剩余项")
        void shouldDeleteGradeBand() {
            when(gradeBandRepository.findByPolicyId(10L))
                    .thenReturn(List.of(band(1L, 10L, "A"), band(2L, 10L, "B")));

            service.deleteGradeBand(10L, 2L);

            verify(gradeBandRepository).deleteByPolicyId(10L);
            // 仅剩 band(1L) 被重写
            verify(gradeBandRepository, times(1)).save(any(PolicyGradeBand.class));
        }

        @Test
        @DisplayName("deleteGradeBand: bandId 不存在 → IllegalArgumentException, 不删除")
        void shouldRejectDeleteBandMissing() {
            when(gradeBandRepository.findByPolicyId(10L))
                    .thenReturn(List.of(band(1L, 10L, "A")));
            assertThatThrownBy(() -> service.deleteGradeBand(10L, 999L))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(gradeBandRepository, never()).deleteByPolicyId(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("PolicyCalcRule CRUD")
    class CalcRuleTests {

        @Test
        @DisplayName("listCalcRules 透传 findByPolicyId")
        void shouldListCalcRules() {
            PolicyCalcRule r = rule(1L, 10L, "R1");
            when(calcRuleRepository.findByPolicyId(10L)).thenReturn(List.of(r));
            assertThat(service.listCalcRules(10L)).containsExactly(r);
        }

        @Test
        @DisplayName("createCalcRule: 校验方案存在后构建并保存")
        void shouldCreateCalcRule() {
            when(policyRepository.findById(10L)).thenReturn(Optional.of(policy(10L, false)));
            ArgumentCaptor<PolicyCalcRule> captor = ArgumentCaptor.forClass(PolicyCalcRule.class);
            when(calcRuleRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            PolicyCalcRule saved = service.createCalcRule(10L, "R9", "规则9",
                    "BONUS", 7, "{\"k\":1}");

            assertThat(saved.getRuleCode()).isEqualTo("R9");
            assertThat(captor.getValue().getPolicyId()).isEqualTo(10L);
            assertThat(captor.getValue().getRuleType()).isEqualTo("BONUS");
            assertThat(captor.getValue().getPriority()).isEqualTo(7);
            assertThat(captor.getValue().getConfig()).isEqualTo("{\"k\":1}");
        }

        @Test
        @DisplayName("createCalcRule: 方案不存在 → IllegalArgumentException")
        void shouldRejectCreateRuleMissingPolicy() {
            when(policyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createCalcRule(99L, "R", "r",
                    "PENALTY", 1, "{}"))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(calcRuleRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateCalcRule: 找到对应规则并重建保存")
        void shouldUpdateCalcRule() {
            when(calcRuleRepository.findByPolicyId(10L))
                    .thenReturn(List.of(rule(1L, 10L, "R1"), rule(2L, 10L, "R2")));
            ArgumentCaptor<PolicyCalcRule> captor = ArgumentCaptor.forClass(PolicyCalcRule.class);
            when(calcRuleRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            PolicyCalcRule saved = service.updateCalcRule(10L, 2L, "R2X", "规则2改",
                    "VETO", 9, "{\"v\":1}", false);

            assertThat(saved.getId()).isEqualTo(2L);
            assertThat(captor.getValue().getRuleCode()).isEqualTo("R2X");
            assertThat(captor.getValue().getRuleType()).isEqualTo("VETO");
            assertThat(captor.getValue().getIsEnabled()).isFalse();
        }

        @Test
        @DisplayName("updateCalcRule: ruleId 不存在 → IllegalArgumentException")
        void shouldRejectUpdateRuleMissing() {
            when(calcRuleRepository.findByPolicyId(10L))
                    .thenReturn(List.of(rule(1L, 10L, "R1")));
            assertThatThrownBy(() -> service.updateCalcRule(10L, 999L, "X", "x",
                    "PENALTY", 1, "{}", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("计算规则不存在");
        }

        @Test
        @DisplayName("deleteCalcRule: 全删后重写剩余项")
        void shouldDeleteCalcRule() {
            when(calcRuleRepository.findByPolicyId(10L))
                    .thenReturn(List.of(rule(1L, 10L, "R1"), rule(2L, 10L, "R2")));

            service.deleteCalcRule(10L, 1L);

            verify(calcRuleRepository).deleteByPolicyId(10L);
            verify(calcRuleRepository, times(1)).save(any(PolicyCalcRule.class));
        }

        @Test
        @DisplayName("deleteCalcRule: ruleId 不存在 → IllegalArgumentException, 不删除")
        void shouldRejectDeleteRuleMissing() {
            when(calcRuleRepository.findByPolicyId(10L))
                    .thenReturn(List.of(rule(1L, 10L, "R1")));
            assertThatThrownBy(() -> service.deleteCalcRule(10L, 999L))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(calcRuleRepository, never()).deleteByPolicyId(any());
        }
    }
}
