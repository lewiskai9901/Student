package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证 ScoringProfile 项目-owned 隔离: 同 sectionId 在不同 projectId 下
 * 各自一套 profile, 互不影响.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringProfile 项目-owned 隔离")
class ScoringProfilePerProjectIsolationTest {

    @Mock ScoringProfileRepository profileRepository;
    @Mock ScoreDimensionRepository dimensionRepository;
    @Mock GradeBandRepository gradeBandRepository;
    @Mock CalculationRuleRepository ruleRepository;
    @Mock EscalationPolicyRepository escalationPolicyRepository;
    @Mock ScoringProfileVersionRepository versionRepository;
    @Mock TemplateSectionRepository sectionRepository;
    @Mock TemplateItemRepository itemRepository;

    ScoringProfileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ScoringProfileApplicationService(
                profileRepository, dimensionRepository, gradeBandRepository,
                ruleRepository, escalationPolicyRepository, versionRepository,
                sectionRepository, itemRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("同 sectionId 在 projectA / projectB 下生成两套独立 profile")
    void shouldKeepProfilesIsolatedAcrossProjects() {
        // 模拟存储: 按 (project, section) 各自维持独立行
        Map<String, ScoringProfile> store = new HashMap<>();
        long[] idGen = {1000L};

        when(profileRepository.findByProjectIdAndSectionId(any(), any()))
                .thenAnswer(inv -> {
                    Long pid = inv.getArgument(0);
                    Long sid = inv.getArgument(1);
                    return Optional.ofNullable(store.get(pid + ":" + sid));
                });
        when(profileRepository.save(any(ScoringProfile.class)))
                .thenAnswer(inv -> {
                    ScoringProfile p = inv.getArgument(0);
                    if (p.getId() == null) p.setId(idGen[0]++);
                    store.put(p.getProjectId() + ":" + p.getSectionId(), p);
                    return p;
                });

        // 项目 A 创建 section=100 的 profile
        ScoringProfile a = service.createProfile(700L, 100L, 1L);
        // 项目 B 创建相同 section=100 的 profile
        ScoringProfile b = service.createProfile(800L, 100L, 1L);

        assertThat(a.getProjectId()).isEqualTo(700L);
        assertThat(b.getProjectId()).isEqualTo(800L);
        assertThat(a.getId()).isNotEqualTo(b.getId());
        assertThat(a.getSectionId()).isEqualTo(b.getSectionId());
        // 同 sectionId 在不同项目下落到不同行
        assertThat(store).hasSize(2);
    }
}
