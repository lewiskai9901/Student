package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.school.management.domain.inspection.model.scoring.CalculationRule;
import com.school.management.domain.inspection.model.scoring.EscalationPolicy;
import com.school.management.domain.inspection.model.scoring.GradeBand;
import com.school.management.domain.inspection.model.scoring.RuleType;
import com.school.management.domain.inspection.model.scoring.ScoreDimension;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.model.scoring.ScoringProfileVersion;
import com.school.management.domain.inspection.model.template.ItemType;
import com.school.management.domain.inspection.model.template.TemplateItem;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.repository.CalculationRuleRepository;
import com.school.management.domain.inspection.repository.EscalationPolicyRepository;
import com.school.management.domain.inspection.repository.GradeBandRepository;
import com.school.management.domain.inspection.repository.ScoreDimensionRepository;
import com.school.management.domain.inspection.repository.ScoringProfileRepository;
import com.school.management.domain.inspection.repository.ScoringProfileVersionRepository;
import com.school.management.domain.inspection.repository.TemplateItemRepository;
import com.school.management.domain.inspection.repository.TemplateSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScoringProfileApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离全部 repository, 验证 ScoringProfile / ScoreDimension /
 * GradeBand / CalculationRule / EscalationPolicy / Version 的编排逻辑,
 * 包括幂等创建、并发兜底、级联删除、维度自动同步、版本快照。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringProfileApplicationService 评分配置应用服务")
class ScoringProfileApplicationServiceTest {

    @Mock ScoringProfileRepository profileRepository;
    @Mock ScoreDimensionRepository dimensionRepository;
    @Mock GradeBandRepository gradeBandRepository;
    @Mock CalculationRuleRepository ruleRepository;
    @Mock EscalationPolicyRepository escalationPolicyRepository;
    @Mock ScoringProfileVersionRepository versionRepository;
    @Mock TemplateSectionRepository sectionRepository;
    @Mock TemplateItemRepository itemRepository;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    ScoringProfileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ScoringProfileApplicationService(
                profileRepository, dimensionRepository, gradeBandRepository,
                ruleRepository, escalationPolicyRepository, versionRepository,
                sectionRepository, itemRepository, objectMapper);
    }

    // ---- helpers ----

    private ScoringProfile profile(Long id, Long sectionId) {
        return ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(id).sectionId(sectionId).projectId(700L).createdBy(1L));
    }

    private ScoringProfile profile(Long id, Long sectionId, Long projectId) {
        return ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(id).sectionId(sectionId).projectId(projectId).createdBy(1L));
    }

    private ScoreDimension dimension(Long id, Long profileId, String code) {
        ScoreDimension d = ScoreDimension.reconstruct(ScoreDimension.builder()
                .id(id).scoringProfileId(profileId).dimensionCode(code)
                .dimensionName("维度" + code).weight(100));
        return d;
    }

    private GradeBand band(Long id, Long profileId) {
        GradeBand b = GradeBand.reconstruct(GradeBand.builder()
                .id(id).scoringProfileId(profileId).gradeCode("A").gradeName("优"));
        return b;
    }

    private CalculationRule rule(Long id, Long profileId) {
        CalculationRule r = CalculationRule.reconstruct(CalculationRule.builder()
                .id(id).scoringProfileId(profileId).ruleCode("R1").ruleName("规则1")
                .ruleType(RuleType.PENALTY));
        return r;
    }

    private EscalationPolicy policy(Long id, Long profileId) {
        EscalationPolicy p = EscalationPolicy.reconstruct(EscalationPolicy.builder()
                .id(id).profileId(profileId).policyName("策略1"));
        return p;
    }

    private TemplateSection childSection(Long id, Long parentId) {
        TemplateSection s = TemplateSection.reconstruct(TemplateSection.builder()
                .sectionCode("SEC-" + id).sectionName("子分区" + id)
                .parentSectionId(parentId).createdBy(1L));
        s.setId(id);
        return s;
    }

    private TemplateItem scoredItem(Long id, Long sectionId, boolean scored) {
        TemplateItem it = TemplateItem.reconstruct(TemplateItem.builder()
                .id(id).sectionId(sectionId).itemCode("IT-" + id).itemName("项" + id)
                .itemType(ItemType.NUMBER).isScored(scored)
                .itemWeight(new BigDecimal("80")).createdBy(1L));
        return it;
    }

    // ============================================================
    @Nested
    @DisplayName("createProfile — 项目-owned 幂等创建")
    class CreateProfileTests {
        @Test
        @DisplayName("(project, section) 已存在: 直接返回现有, 不再 save")
        void shouldReturnExisting() {
            ScoringProfile existing = profile(500L, 100L, 700L);
            when(profileRepository.findByProjectIdAndSectionId(700L, 100L))
                    .thenReturn(Optional.of(existing));

            ScoringProfile result = service.createProfile(700L, 100L, 1L);

            assertThat(result).isSameAs(existing);
            verify(profileRepository, never()).save(any(ScoringProfile.class));
        }

        @Test
        @DisplayName("不存在: 创建并保存新 profile, projectId 已写入")
        void shouldCreateNew() {
            when(profileRepository.findByProjectIdAndSectionId(700L, 100L))
                    .thenReturn(Optional.empty());
            when(profileRepository.save(any(ScoringProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScoringProfile result = service.createProfile(700L, 100L, 9L);

            assertThat(result.getSectionId()).isEqualTo(100L);
            assertThat(result.getProjectId()).isEqualTo(700L);
            assertThat(result.getCreatedBy()).isEqualTo(9L);
            verify(profileRepository).save(any(ScoringProfile.class));
        }

        @Test
        @DisplayName("projectId 为空: 拒绝创建")
        void shouldRejectNullProjectId() {
            assertThatThrownBy(() -> service.createProfile(null, 100L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("projectId 必传");
        }

        @Test
        @DisplayName("并发 DuplicateKeyException: 复查命中, 返回他人创建的")
        void shouldFallbackOnRaceWhenRefindHits() {
            ScoringProfile other = profile(501L, 100L, 700L);
            when(profileRepository.findByProjectIdAndSectionId(700L, 100L))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(other));
            when(profileRepository.save(any(ScoringProfile.class)))
                    .thenThrow(new DuplicateKeyException("dup"));

            ScoringProfile result = service.createProfile(700L, 100L, 1L);

            assertThat(result).isSameAs(other);
        }

        @Test
        @DisplayName("并发 DuplicateKeyException 但复查未命中: 抛 IllegalStateException")
        void shouldThrowWhenRaceAndRefindMisses() {
            when(profileRepository.findByProjectIdAndSectionId(700L, 100L))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.empty());
            when(profileRepository.save(any(ScoringProfile.class)))
                    .thenThrow(new DuplicateKeyException("dup"));

            assertThatThrownBy(() -> service.createProfile(700L, 100L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("并发竞争");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getProfile / getProfileByProjectIdAndSectionId / listByProjectId — 查询")
    class GetProfileTests {
        @Test
        @DisplayName("getProfile: 透传 findById")
        void shouldGetById() {
            when(profileRepository.findById(500L)).thenReturn(Optional.of(profile(500L, 100L)));
            assertThat(service.getProfile(500L)).isPresent();
        }

        @Test
        @DisplayName("getProfile: 不存在返回 empty")
        void shouldGetByIdEmpty() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThat(service.getProfile(9L)).isEmpty();
        }

        @Test
        @DisplayName("getProfileByProjectIdAndSectionId: 透传 findByProjectIdAndSectionId")
        void shouldGetByProjectAndSection() {
            when(profileRepository.findByProjectIdAndSectionId(700L, 100L))
                    .thenReturn(Optional.of(profile(500L, 100L, 700L)));
            assertThat(service.getProfileByProjectIdAndSectionId(700L, 100L)).isPresent();
        }

        @Test
        @DisplayName("listByProjectId: 透传 findByProjectId")
        void shouldListByProject() {
            when(profileRepository.findByProjectId(700L)).thenReturn(
                    List.of(profile(1L, 1L, 700L), profile(2L, 2L, 700L)));
            assertThat(service.listByProjectId(700L)).hasSize(2);
        }

        @Test
        @DisplayName("listByProjectId: projectId 为空抛异常")
        void shouldRejectNullProjectIdOnList() {
            assertThatThrownBy(() -> service.listByProjectId(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("updateProfile / updateAdvancedSettings — 更新")
    class UpdateProfileTests {
        @Test
        @DisplayName("updateProfile: 写入 maxScore/minScore/precision")
        void shouldUpdateProfile() {
            ScoringProfile p = profile(500L, 100L);
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            when(profileRepository.save(any(ScoringProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScoringProfile result = service.updateProfile(
                    500L, new BigDecimal("120"), new BigDecimal("10"), 3,
                    null, null, null, null, null, 88L);

            assertThat(result.getMaxScore()).isEqualByComparingTo("120");
            assertThat(result.getMinScore()).isEqualByComparingTo("10");
            assertThat(result.getPrecisionDigits()).isEqualTo(3);
            assertThat(result.getUpdatedBy()).isEqualTo(88L);
        }

        @Test
        @DisplayName("updateProfile: profile 不存在抛 IllegalArgumentException")
        void shouldRejectUpdateMissing() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateProfile(
                    9L, BigDecimal.TEN, BigDecimal.ONE, 2,
                    null, null, null, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("评分配置不存在");
        }

        @Test
        @DisplayName("updateProfile: minScore > maxScore 抛 IllegalArgumentException")
        void shouldRejectMinGreaterThanMax() {
            ScoringProfile p = profile(500L, 100L);
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> service.updateProfile(
                    500L, new BigDecimal("10"), new BigDecimal("99"), 2,
                    null, null, null, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("updateAdvancedSettings: 写入趋势/衰减/多评审员/校准字段")
        void shouldUpdateAdvanced() {
            ScoringProfile p = profile(500L, 100L, 700L);
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            when(profileRepository.save(any(ScoringProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScoringProfile result = service.updateAdvancedSettings(500L, 700L,
                    true, 14, new BigDecimal("0.5"), new BigDecimal("0.3"), new BigDecimal("5"),
                    true, "LINEAR", new BigDecimal("0.1"), new BigDecimal("60"),
                    "AVERAGE", "EQUAL", new BigDecimal("0.8"),
                    true, "Z_SCORE", 30, 10, 88L);

            assertThat(result.getTrendFactorEnabled()).isTrue();
            assertThat(result.getTrendLookbackDays()).isEqualTo(14);
            assertThat(result.getDecayMode()).isEqualTo("LINEAR");
            assertThat(result.getMultiRaterMode()).isEqualTo("AVERAGE");
            assertThat(result.getCalibrationMethod()).isEqualTo("Z_SCORE");
        }

        @Test
        @DisplayName("updateAdvancedSettings: profile 不存在抛异常")
        void shouldRejectAdvancedMissing() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateAdvancedSettings(9L, null,
                    false, 7, null, null, null, false, null, null, null,
                    null, null, null, false, null, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("updateAdvancedSettings: expectedProjectId 与归属不符抛异常")
        void shouldRejectAdvancedWrongProject() {
            ScoringProfile p = profile(500L, 100L, 700L);
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> service.updateAdvancedSettings(500L, 999L,
                    false, 7, null, null, null, false, null, null, null,
                    null, null, null, false, null, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("禁止跨项目修改");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("cloneForProject — 深拷贝到目标项目")
    class CloneForProjectTests {
        @Test
        @DisplayName("拷贝 profile + dimensions + bands + rules, projectId 改为目标")
        void shouldDeepCopy() {
            ScoringProfile src = profile(500L, 100L, 700L);
            when(profileRepository.findById(500L)).thenReturn(Optional.of(src));
            when(profileRepository.save(any(ScoringProfile.class)))
                    .thenAnswer(inv -> {
                        ScoringProfile p = inv.getArgument(0);
                        if (p.getId() == null) p.setId(999L);
                        return p;
                    });
            ScoreDimension srcDim = dimension(20L, 500L, "D1");
            when(dimensionRepository.findByScoringProfileId(500L)).thenReturn(List.of(srcDim));
            when(dimensionRepository.save(any(ScoreDimension.class)))
                    .thenAnswer(inv -> {
                        ScoreDimension d = inv.getArgument(0);
                        if (d.getId() == null) d.setId(2000L);
                        return d;
                    });
            GradeBand srcBand = GradeBand.reconstruct(GradeBand.builder()
                    .id(30L).scoringProfileId(500L).dimensionId(20L)
                    .gradeCode("A").gradeName("优"));
            when(gradeBandRepository.findByScoringProfileId(500L)).thenReturn(List.of(srcBand));
            when(gradeBandRepository.save(any(GradeBand.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            CalculationRule srcRule = rule(40L, 500L);
            when(ruleRepository.findByScoringProfileIdOrderByPriority(500L))
                    .thenReturn(List.of(srcRule));
            when(ruleRepository.save(any(CalculationRule.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScoringProfile clone = service.cloneForProject(500L, 888L, 9L);

            assertThat(clone.getProjectId()).isEqualTo(888L);
            assertThat(clone.getSectionId()).isEqualTo(100L);
            // 应当 save 1 个 profile + 1 个 dim + 1 个 band + 1 个 rule
            verify(dimensionRepository, times(1)).save(any(ScoreDimension.class));
            verify(gradeBandRepository, times(1)).save(any(GradeBand.class));
            verify(ruleRepository, times(1)).save(any(CalculationRule.class));
        }

        @Test
        @DisplayName("newProjectId 为空抛异常")
        void shouldRejectNullTargetProject() {
            assertThatThrownBy(() -> service.cloneForProject(500L, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("源 profile 不存在抛异常")
        void shouldRejectMissingSource() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.cloneForProject(9L, 888L, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("deleteProfile — 级联删除")
    class DeleteProfileTests {
        @Test
        @DisplayName("级联清 rule/gradeBand/dimension 后删 profile")
        void shouldCascadeDelete() {
            service.deleteProfile(500L);

            verify(ruleRepository).deleteByScoringProfileId(500L);
            verify(gradeBandRepository).deleteByScoringProfileId(500L);
            verify(dimensionRepository).deleteByScoringProfileId(500L);
            verify(profileRepository).deleteById(500L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("ScoreDimension — CRUD")
    class DimensionTests {
        @Test
        @DisplayName("createDimension: profile 存在时保存维度")
        void shouldCreateDimension() {
            when(profileRepository.findById(500L)).thenReturn(Optional.of(profile(500L, 100L)));
            when(dimensionRepository.save(any(ScoreDimension.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScoreDimension result = service.createDimension(500L, "D1", "维度1",
                    50, new BigDecimal("100"), new BigDecimal("60"), 1);

            assertThat(result.getDimensionCode()).isEqualTo("D1");
            assertThat(result.getDimensionName()).isEqualTo("维度1");
            assertThat(result.getWeight()).isEqualTo(50);
        }

        @Test
        @DisplayName("createDimension: profile 不存在抛异常")
        void shouldRejectCreateDimensionMissingProfile() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createDimension(9L, "D", "n",
                    1, BigDecimal.ONE, null, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("listDimensions: 透传 findByScoringProfileId")
        void shouldListDimensions() {
            when(dimensionRepository.findByScoringProfileId(500L))
                    .thenReturn(List.of(dimension(1L, 500L, "A")));
            assertThat(service.listDimensions(500L)).hasSize(1);
        }

        @Test
        @DisplayName("updateDimension: 更新名称/权重并保存")
        void shouldUpdateDimension() {
            ScoreDimension d = dimension(10L, 500L, "A");
            when(dimensionRepository.findById(10L)).thenReturn(Optional.of(d));
            when(dimensionRepository.save(any(ScoreDimension.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScoreDimension result = service.updateDimension(10L, "改名",
                    70, new BigDecimal("90"), new BigDecimal("55"));

            assertThat(result.getDimensionName()).isEqualTo("改名");
            assertThat(result.getWeight()).isEqualTo(70);
        }

        @Test
        @DisplayName("updateDimension: 维度不存在抛异常")
        void shouldRejectUpdateDimensionMissing() {
            when(dimensionRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateDimension(9L, "n", 1, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("评分维度不存在");
        }

        @Test
        @DisplayName("deleteDimension: 先清 gradeBand 再删维度")
        void shouldDeleteDimension() {
            service.deleteDimension(10L);
            verify(gradeBandRepository).deleteByDimensionId(10L);
            verify(dimensionRepository).deleteById(10L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("syncAllDimensions — 自动同步维度")
    class SyncDimensionsTests {
        @Test
        @DisplayName("新增子分区: 创建对应 SEC_ 维度")
        void shouldCreateDimensionForNewSection() {
            ScoringProfile p = profile(500L, 100L);
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            when(dimensionRepository.findByScoringProfileId(500L))
                    .thenReturn(List.of())
                    .thenReturn(List.of(dimension(1L, 500L, "SEC_2")));
            when(sectionRepository.findByParentSectionId(100L))
                    .thenReturn(List.of(childSection(2L, 100L)));
            when(itemRepository.findBySectionId(100L)).thenReturn(List.of());

            List<ScoreDimension> result = service.syncAllDimensions(500L);

            assertThat(result).hasSize(1);
            // 新增一个 SEC_2 维度
            verify(dimensionRepository, times(1)).save(any(ScoreDimension.class));
        }

        @Test
        @DisplayName("仅计分字段 (isScored=true) 生成 ITEM_ 维度, 非计分字段跳过")
        void shouldOnlySyncScoredItems() {
            ScoringProfile p = profile(500L, 100L);
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            when(dimensionRepository.findByScoringProfileId(500L))
                    .thenReturn(List.of())
                    .thenReturn(List.of(dimension(1L, 500L, "ITEM_20")));
            when(sectionRepository.findByParentSectionId(100L)).thenReturn(List.of());
            when(itemRepository.findBySectionId(100L)).thenReturn(List.of(
                    scoredItem(20L, 100L, true),
                    scoredItem(21L, 100L, false)));

            service.syncAllDimensions(500L);

            // 仅计分项 20 生成维度
            verify(dimensionRepository, times(1)).save(any(ScoreDimension.class));
        }

        @Test
        @DisplayName("已存在维度不再对应任何活跃 code: 删除")
        void shouldDeleteStaleDimension() {
            ScoringProfile p = profile(500L, 100L);
            ScoreDimension stale = dimension(99L, 500L, "SEC_999");
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            when(dimensionRepository.findByScoringProfileId(500L))
                    .thenReturn(List.of(stale))
                    .thenReturn(List.of());
            when(sectionRepository.findByParentSectionId(100L)).thenReturn(List.of());
            when(itemRepository.findBySectionId(100L)).thenReturn(List.of());

            service.syncAllDimensions(500L);

            verify(gradeBandRepository).deleteByDimensionId(99L);
            verify(dimensionRepository).deleteById(99L);
        }

        @Test
        @DisplayName("已存在的 SEC_ 维度: 更新而不是新建/删除")
        void shouldUpdateExistingDimension() {
            ScoringProfile p = profile(500L, 100L);
            ScoreDimension existing = dimension(7L, 500L, "SEC_2");
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            when(dimensionRepository.findByScoringProfileId(500L))
                    .thenReturn(List.of(existing))
                    .thenReturn(List.of(existing));
            when(sectionRepository.findByParentSectionId(100L))
                    .thenReturn(List.of(childSection(2L, 100L)));
            when(itemRepository.findBySectionId(100L)).thenReturn(List.of());

            service.syncAllDimensions(500L);

            // 维度被 update + save, 但不删除
            verify(dimensionRepository, times(1)).save(existing);
            verify(dimensionRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("profile 不存在抛异常")
        void shouldRejectSyncMissingProfile() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.syncAllDimensions(9L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("GradeBand — CRUD")
    class GradeBandTests {
        @Test
        @DisplayName("createGradeBand: profile 存在时保存等级区间")
        void shouldCreateGradeBand() {
            when(profileRepository.findById(500L)).thenReturn(Optional.of(profile(500L, 100L)));
            when(gradeBandRepository.save(any(GradeBand.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            GradeBand result = service.createGradeBand(500L, 10L, "A", "优秀",
                    new BigDecimal("90"), new BigDecimal("100"), "#0f0", "star", 1);

            assertThat(result.getGradeCode()).isEqualTo("A");
            assertThat(result.getGradeName()).isEqualTo("优秀");
            assertThat(result.getMinScore()).isEqualByComparingTo("90");
        }

        @Test
        @DisplayName("createGradeBand: profile 不存在抛异常")
        void shouldRejectCreateGradeBandMissingProfile() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createGradeBand(9L, null, "A", "优",
                    BigDecimal.ZERO, BigDecimal.TEN, null, null, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("listGradeBands: 透传 findByScoringProfileId")
        void shouldListGradeBands() {
            when(gradeBandRepository.findByScoringProfileId(500L))
                    .thenReturn(List.of(band(1L, 500L)));
            assertThat(service.listGradeBands(500L)).hasSize(1);
        }

        @Test
        @DisplayName("updateGradeBand: 更新名称/区间并保存")
        void shouldUpdateGradeBand() {
            GradeBand b = band(20L, 500L);
            when(gradeBandRepository.findById(20L)).thenReturn(Optional.of(b));
            when(gradeBandRepository.save(any(GradeBand.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            GradeBand result = service.updateGradeBand(20L, "良好",
                    new BigDecimal("70"), new BigDecimal("89"), "#ff0", "ok");

            assertThat(result.getGradeName()).isEqualTo("良好");
            assertThat(result.getMaxScore()).isEqualByComparingTo("89");
        }

        @Test
        @DisplayName("updateGradeBand: 区间不存在抛异常")
        void shouldRejectUpdateGradeBandMissing() {
            when(gradeBandRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateGradeBand(9L, "n",
                    BigDecimal.ZERO, BigDecimal.TEN, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("等级区间不存在");
        }

        @Test
        @DisplayName("deleteGradeBand: 透传 deleteById")
        void shouldDeleteGradeBand() {
            service.deleteGradeBand(20L);
            verify(gradeBandRepository).deleteById(20L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("CalculationRule — CRUD")
    class CalculationRuleTests {
        @Test
        @DisplayName("createRule: profile 存在时保存规则")
        void shouldCreateRule() {
            when(profileRepository.findById(500L)).thenReturn(Optional.of(profile(500L, 100L)));
            when(ruleRepository.save(any(CalculationRule.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CalculationRule result = service.createRule(500L, "R1", "规则1", 5,
                    RuleType.PENALTY, "{}", true, "GLOBAL", null, null, null,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "grp");

            assertThat(result.getRuleCode()).isEqualTo("R1");
            assertThat(result.getRuleType()).isEqualTo(RuleType.PENALTY);
            assertThat(result.getPriority()).isEqualTo(5);
            assertThat(result.getExclusionGroup()).isEqualTo("grp");
        }

        @Test
        @DisplayName("createRule: profile 不存在抛异常")
        void shouldRejectCreateRuleMissingProfile() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createRule(9L, "R", "n", 0,
                    RuleType.BONUS, null, true, null, null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("listRules: 透传 findByScoringProfileIdOrderByPriority")
        void shouldListRules() {
            when(ruleRepository.findByScoringProfileIdOrderByPriority(500L))
                    .thenReturn(List.of(rule(1L, 500L)));
            assertThat(service.listRules(500L)).hasSize(1);
        }

        @Test
        @DisplayName("updateRule: 更新名称/优先级并保存")
        void shouldUpdateRule() {
            CalculationRule r = rule(30L, 500L);
            when(ruleRepository.findById(30L)).thenReturn(Optional.of(r));
            when(ruleRepository.save(any(CalculationRule.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CalculationRule result = service.updateRule(30L, "改名规则", 9,
                    RuleType.VETO, "{\"x\":1}", false, "DIMENSION", "[1]",
                    null, null, null, null, null);

            assertThat(result.getRuleName()).isEqualTo("改名规则");
            assertThat(result.getPriority()).isEqualTo(9);
            assertThat(result.getRuleType()).isEqualTo(RuleType.VETO);
            assertThat(result.getIsEnabled()).isFalse();
        }

        @Test
        @DisplayName("updateRule: 规则不存在抛异常")
        void shouldRejectUpdateRuleMissing() {
            when(ruleRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateRule(9L, "n", 0,
                    RuleType.CUSTOM, null, true, null, null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("计算规则不存在");
        }

        @Test
        @DisplayName("deleteRule: 透传 deleteById")
        void shouldDeleteRule() {
            service.deleteRule(30L);
            verify(ruleRepository).deleteById(30L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("EscalationPolicy — CRUD")
    class EscalationPolicyTests {
        @Test
        @DisplayName("createEscalationPolicy: profile 存在时保存策略")
        void shouldCreatePolicy() {
            when(profileRepository.findById(500L)).thenReturn(Optional.of(profile(500L, 100L)));
            when(escalationPolicyRepository.save(any(EscalationPolicy.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EscalationPolicy result = service.createEscalationPolicy(500L, "递增策略",
                    30, "MULTIPLY", new BigDecimal("2"), null, null,
                    new BigDecimal("5"), "ITEM_CODE", true);

            assertThat(result.getPolicyName()).isEqualTo("递增策略");
            assertThat(result.getEscalationMode()).isEqualTo("MULTIPLY");
            assertThat(result.getMultiplier()).isEqualByComparingTo("2");
        }

        @Test
        @DisplayName("createEscalationPolicy: profile 不存在抛异常")
        void shouldRejectCreatePolicyMissingProfile() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createEscalationPolicy(9L, "p",
                    30, "ADD", null, BigDecimal.ONE, null, null, "DIMENSION", true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("listEscalationPolicies: 透传 findByProfileId")
        void shouldListPolicies() {
            when(escalationPolicyRepository.findByProfileId(500L))
                    .thenReturn(List.of(policy(1L, 500L)));
            assertThat(service.listEscalationPolicies(500L)).hasSize(1);
        }

        @Test
        @DisplayName("updateEscalationPolicy: 更新并保存")
        void shouldUpdatePolicy() {
            EscalationPolicy p = policy(40L, 500L);
            when(escalationPolicyRepository.findById(40L)).thenReturn(Optional.of(p));
            when(escalationPolicyRepository.save(any(EscalationPolicy.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EscalationPolicy result = service.updateEscalationPolicy(40L, "改名策略",
                    60, "ADD", null, new BigDecimal("3"), null,
                    new BigDecimal("8"), "DIMENSION", false);

            assertThat(result.getPolicyName()).isEqualTo("改名策略");
            assertThat(result.getEscalationMode()).isEqualTo("ADD");
            assertThat(result.getIsEnabled()).isFalse();
        }

        @Test
        @DisplayName("updateEscalationPolicy: 策略不存在抛异常")
        void shouldRejectUpdatePolicyMissing() {
            when(escalationPolicyRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateEscalationPolicy(9L, "n",
                    30, "MULTIPLY", BigDecimal.ONE, null, null, null, "ITEM_CODE", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("递增策略不存在");
        }

        @Test
        @DisplayName("deleteEscalationPolicy: 透传 deleteById")
        void shouldDeletePolicy() {
            service.deleteEscalationPolicy(40L);
            verify(escalationPolicyRepository).deleteById(40L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("publishVersion / listVersions / getVersion — 版本管理")
    class VersionTests {
        @Test
        @DisplayName("publishVersion: 收集子资源快照, 递增版本号, 保存版本")
        void shouldPublishVersion() {
            ScoringProfile p = profile(500L, 100L);
            when(profileRepository.findById(500L)).thenReturn(Optional.of(p));
            when(dimensionRepository.findByScoringProfileId(500L))
                    .thenReturn(List.of(dimension(1L, 500L, "A")));
            when(gradeBandRepository.findByScoringProfileId(500L))
                    .thenReturn(List.of(band(2L, 500L)));
            when(ruleRepository.findByScoringProfileIdOrderByPriority(500L))
                    .thenReturn(List.of(rule(3L, 500L)));
            when(escalationPolicyRepository.findByProfileId(500L))
                    .thenReturn(List.of(policy(4L, 500L)));
            when(profileRepository.save(any(ScoringProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(versionRepository.save(any(ScoringProfileVersion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScoringProfileVersion result = service.publishVersion(500L, "首次发布", 77L);

            assertThat(result.getProfileId()).isEqualTo(500L);
            assertThat(result.getVersion()).isEqualTo(1); // 从 0 递增到 1
            assertThat(result.getChangeSummary()).isEqualTo("首次发布");
            assertThat(result.getPublishedBy()).isEqualTo(77L);
            assertThat(result.getSnapshot()).contains("profile").contains("dimensions");
            verify(profileRepository).save(p);
        }

        @Test
        @DisplayName("publishVersion: profile 不存在抛异常")
        void shouldRejectPublishMissing() {
            when(profileRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.publishVersion(9L, "x", 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("listVersions: 透传 findByProfileId")
        void shouldListVersions() {
            ScoringProfileVersion v = ScoringProfileVersion.reconstruct(
                    ScoringProfileVersion.builder().id(1L).profileId(500L).version(1));
            when(versionRepository.findByProfileId(500L)).thenReturn(List.of(v));
            assertThat(service.listVersions(500L)).hasSize(1);
        }

        @Test
        @DisplayName("getVersion: 透传 profileId + version")
        void shouldGetVersion() {
            ScoringProfileVersion v = ScoringProfileVersion.reconstruct(
                    ScoringProfileVersion.builder().id(1L).profileId(500L).version(2));
            when(versionRepository.findByProfileIdAndVersion(500L, 2))
                    .thenReturn(Optional.of(v));
            Optional<ScoringProfileVersion> result = service.getVersion(500L, 2);
            assertThat(result).isPresent();
            assertThat(result.get().getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("getVersion: 不存在返回 empty")
        void shouldGetVersionEmpty() {
            when(versionRepository.findByProfileIdAndVersion(500L, 9))
                    .thenReturn(Optional.empty());
            assertThat(service.getVersion(500L, 9)).isEmpty();
        }
    }
}
