package com.school.management.infrastructure.extension.plugins.education.application.academic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.infrastructure.extension.plugins.education.application.academic.command.CreateCurriculumPlanCommand;
import com.school.management.infrastructure.extension.plugins.education.application.academic.command.CreatePlanCourseCommand;
import com.school.management.infrastructure.extension.plugins.education.application.academic.command.UpdateCurriculumPlanCommand;
import com.school.management.infrastructure.extension.plugins.education.application.academic.command.UpdatePlanCourseCommand;
import com.school.management.infrastructure.extension.plugins.education.application.academic.query.CurriculumPlanDTO;
import com.school.management.infrastructure.extension.plugins.education.application.academic.query.PlanCourseDTO;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.CurriculumPlan;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.PlanCourse;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.repository.CurriculumPlanRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.repository.PlanCourseRepository;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.academic.CurriculumPlanPO;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.academic.CurriculumPlanPersistenceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
 * CurriculumPlanApplicationService 单测 — 验证培养方案 / 方案课程 CRUD 与命令编排
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CurriculumPlanApplicationService 测试")
class CurriculumPlanApplicationServiceTest {

    @Mock
    private CurriculumPlanRepository planRepository;

    @Mock
    private PlanCourseRepository planCourseRepository;

    @Mock
    private CurriculumPlanPersistenceMapper planMapper;

    @InjectMocks
    private CurriculumPlanApplicationService service;

    // ---- helpers ----

    private CurriculumPlan buildPlan(Long id) {
        return CurriculumPlan.builder()
            .id(id)
            .planCode("PLAN-001")
            .planName("软件工程培养方案")
            .majorId(10L)
            .majorDirectionId(20L)
            .gradeYear(2024)
            .totalCredits(new BigDecimal("160"))
            .requiredCredits(new BigDecimal("120"))
            .electiveCredits(new BigDecimal("30"))
            .practiceCredits(new BigDecimal("10"))
            .trainingObjective("培养目标")
            .graduationRequirement("毕业要求")
            .planVersion(1)
            .status(0)
            .createdBy(99L)
            .build();
    }

    private CurriculumPlanPO buildPO(Long id) {
        CurriculumPlanPO po = new CurriculumPlanPO();
        po.setId(id);
        po.setPlanCode("PLAN-001");
        po.setPlanName("软件工程培养方案");
        po.setMajorId(10L);
        po.setGradeYear(2024);
        po.setVersion(2);
        po.setStatus(1);
        return po;
    }

    private PlanCourse buildPlanCourse(Long id, Long planId) {
        return PlanCourse.builder()
            .id(id)
            .planId(planId)
            .courseId(500L)
            .semesterNumber(3)
            .courseCategory(1)
            .courseType(2)
            .credits(new BigDecimal("4"))
            .totalHours(64)
            .weeklyHours(4)
            .theoryHours(48)
            .practiceHours(16)
            .assessmentMethod(1)
            .sortOrder(5)
            .remark("备注")
            .courseCode("CS-101")
            .courseName("数据结构")
            .build();
    }

    @Nested
    @DisplayName("方案查询")
    class PlanQueryTests {

        @Test
        @DisplayName("getPlanList 应分页查询并将 PO 映射为 DTO")
        void shouldReturnPagedDtos() {
            Page<CurriculumPlanPO> poPage = new Page<>(1, 10, 1);
            poPage.setRecords(List.of(buildPO(1L)));
            when(planMapper.selectPage(any(Page.class), any())).thenReturn(poPage);

            Page<CurriculumPlanDTO> result = service.getPlanList(2024, 1, 10L, 1, 10);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
            CurriculumPlanDTO dto = result.getRecords().get(0);
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getPlanCode()).isEqualTo("PLAN-001");
            assertThat(dto.getVersion()).isEqualTo(2);
            assertThat(dto.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("getPlanList 所有过滤参数为 null 时仍能查询并返回空记录页")
        void shouldQueryWithoutFiltersWhenParamsNull() {
            Page<CurriculumPlanPO> poPage = new Page<>(1, 10, 0);
            poPage.setRecords(List.of());
            when(planMapper.selectPage(any(Page.class), any())).thenReturn(poPage);

            Page<CurriculumPlanDTO> result = service.getPlanList(null, null, null, 1, 10);

            assertThat(result.getRecords()).isEmpty();
            verify(planMapper).selectPage(any(Page.class), any());
        }

        @Test
        @DisplayName("getPlan 存在时返回映射后的 DTO")
        void shouldReturnPlanDto() {
            when(planRepository.findById(1L)).thenReturn(Optional.of(buildPlan(1L)));

            CurriculumPlanDTO dto = service.getPlan(1L);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getPlanName()).isEqualTo("软件工程培养方案");
            assertThat(dto.getTotalCredits()).isEqualByComparingTo("160");
            assertThat(dto.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("getPlan 不存在时抛 IllegalArgumentException")
        void shouldThrowWhenPlanNotFound() {
            when(planRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPlan(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("培养方案不存在");
        }
    }

    @Nested
    @DisplayName("方案命令")
    class PlanCommandTests {

        @Test
        @DisplayName("createPlan 应按命令字段构建聚合并保存")
        void shouldCreatePlan() {
            CreateCurriculumPlanCommand cmd = new CreateCurriculumPlanCommand();
            cmd.setPlanCode("PLAN-NEW");
            cmd.setPlanName("新方案");
            cmd.setMajorId(11L);
            cmd.setMajorDirectionId(21L);
            cmd.setGradeYear(2025);
            cmd.setTotalCredits(new BigDecimal("150"));
            cmd.setRequiredCredits(new BigDecimal("100"));
            cmd.setElectiveCredits(new BigDecimal("40"));
            cmd.setPracticeCredits(new BigDecimal("10"));
            cmd.setTrainingObjective("目标");
            cmd.setGraduationRequirement("要求");
            cmd.setStatus(0);
            cmd.setCreatedBy(7L);

            when(planRepository.save(any(CurriculumPlan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            CurriculumPlanDTO dto = service.createPlan(cmd);

            ArgumentCaptor<CurriculumPlan> cap = ArgumentCaptor.forClass(CurriculumPlan.class);
            verify(planRepository).save(cap.capture());
            CurriculumPlan saved = cap.getValue();
            assertThat(saved.getPlanCode()).isEqualTo("PLAN-NEW");
            assertThat(saved.getPlanName()).isEqualTo("新方案");
            assertThat(saved.getMajorId()).isEqualTo(11L);
            assertThat(saved.getCreatedBy()).isEqualTo(7L);
            assertThat(dto.getPlanCode()).isEqualTo("PLAN-NEW");
        }

        @Test
        @DisplayName("updatePlan 存在时应更新字段并保存")
        void shouldUpdatePlan() {
            CurriculumPlan plan = buildPlan(1L);
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
            when(planRepository.save(any(CurriculumPlan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            UpdateCurriculumPlanCommand cmd = new UpdateCurriculumPlanCommand();
            cmd.setPlanName("改名后的方案");
            cmd.setMajorId(99L);
            cmd.setGradeYear(2026);
            cmd.setTotalCredits(new BigDecimal("170"));
            cmd.setUpdatedBy(8L);

            CurriculumPlanDTO dto = service.updatePlan(1L, cmd);

            assertThat(dto.getPlanName()).isEqualTo("改名后的方案");
            assertThat(dto.getGradeYear()).isEqualTo(2026);
            assertThat(dto.getTotalCredits()).isEqualByComparingTo("170");
            verify(planRepository).save(plan);
        }

        @Test
        @DisplayName("updatePlan 不存在时抛异常且不保存")
        void shouldThrowWhenUpdateMissingPlan() {
            when(planRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePlan(404L, new UpdateCurriculumPlanCommand()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("培养方案不存在");
            verify(planRepository, never()).save(any(CurriculumPlan.class));
        }

        @Test
        @DisplayName("deletePlan 应先删方案课程再删方案")
        void shouldDeletePlanAndCourses() {
            service.deletePlan(5L);

            verify(planCourseRepository).deleteByPlanId(5L);
            verify(planRepository).deleteById(5L);
        }

        @Test
        @DisplayName("publishPlan 存在时应将状态置为已发布并保存")
        void shouldPublishPlan() {
            CurriculumPlan plan = buildPlan(1L);
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

            service.publishPlan(1L, 88L);

            assertThat(plan.getStatus()).isEqualTo(1);
            assertThat(plan.getPublishedBy()).isEqualTo(88L);
            assertThat(plan.getPublishedAt()).isNotNull();
            verify(planRepository).save(plan);
        }

        @Test
        @DisplayName("publishPlan 不存在时抛异常且不保存")
        void shouldThrowWhenPublishMissingPlan() {
            when(planRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishPlan(404L, 88L))
                .isInstanceOf(IllegalArgumentException.class);
            verify(planRepository, never()).save(any(CurriculumPlan.class));
        }

        @Test
        @DisplayName("deprecatePlan 存在时应将状态置为已归档并保存")
        void shouldDeprecatePlan() {
            CurriculumPlan plan = buildPlan(1L);
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

            service.deprecatePlan(1L);

            assertThat(plan.getStatus()).isEqualTo(2);
            verify(planRepository).save(plan);
        }

        @Test
        @DisplayName("deprecatePlan 不存在时抛异常")
        void shouldThrowWhenDeprecateMissingPlan() {
            when(planRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deprecatePlan(404L))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("copyPlan 应基于已有最大版本号生成新版本并复制全部课程")
        void shouldCopyPlanWithIncrementedVersion() {
            CurriculumPlan original = buildPlan(1L);
            when(planRepository.findById(1L)).thenReturn(Optional.of(original));
            when(planRepository.findMaxVersionByPlanCode("PLAN-001")).thenReturn(3);
            when(planRepository.save(any(CurriculumPlan.class)))
                .thenAnswer(inv -> {
                    CurriculumPlan p = inv.getArgument(0);
                    return CurriculumPlan.builder()
                        .id(777L)
                        .planCode(p.getPlanCode())
                        .planName(p.getPlanName())
                        .planVersion(p.getPlanVersion())
                        .build();
                });
            when(planCourseRepository.findByPlanId(1L))
                .thenReturn(List.of(buildPlanCourse(10L, 1L), buildPlanCourse(11L, 1L)));
            when(planCourseRepository.save(any(PlanCourse.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = service.copyPlan(1L, 5L);

            assertThat(result.get("id")).isEqualTo(777L);
            assertThat(result.get("version")).isEqualTo(4);
            assertThat(result.get("copiedCourses")).isEqualTo(2);
            verify(planCourseRepository, times(2)).save(any(PlanCourse.class));
        }

        @Test
        @DisplayName("copyPlan 无历史版本时新版本号应为 2")
        void shouldCopyPlanWithDefaultVersionWhenNoMaxVersion() {
            CurriculumPlan original = buildPlan(1L);
            when(planRepository.findById(1L)).thenReturn(Optional.of(original));
            when(planRepository.findMaxVersionByPlanCode("PLAN-001")).thenReturn(null);
            when(planRepository.save(any(CurriculumPlan.class)))
                .thenAnswer(inv -> CurriculumPlan.builder()
                    .id(800L).planCode("PLAN-001").planName("软件工程培养方案").build());
            when(planCourseRepository.findByPlanId(1L)).thenReturn(List.of());

            Map<String, Object> result = service.copyPlan(1L, 5L);

            assertThat(result.get("version")).isEqualTo(2);
            assertThat(result.get("copiedCourses")).isEqualTo(0);
            verify(planCourseRepository, never()).save(any(PlanCourse.class));
        }

        @Test
        @DisplayName("copyPlan 源方案不存在时抛异常")
        void shouldThrowWhenCopyMissingPlan() {
            when(planRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.copyPlan(404L, 5L))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("方案课程")
    class PlanCourseTests {

        @Test
        @DisplayName("getPlanCourses 应返回映射后的课程 DTO 列表")
        void shouldListPlanCourses() {
            when(planCourseRepository.findByPlanId(1L))
                .thenReturn(Arrays.asList(buildPlanCourse(10L, 1L), buildPlanCourse(11L, 1L)));

            List<PlanCourseDTO> result = service.getPlanCourses(1L);

            assertThat(result).hasSize(2);
            PlanCourseDTO dto = result.get(0);
            assertThat(dto.getId()).isEqualTo(10L);
            assertThat(dto.getCourseName()).isEqualTo("数据结构");
            assertThat(dto.getCourseCode()).isEqualTo("CS-101");
            assertThat(dto.getCredits()).isEqualByComparingTo("4");
        }

        @Test
        @DisplayName("getPlanCourses 无课程时返回空列表")
        void shouldReturnEmptyWhenNoCourses() {
            when(planCourseRepository.findByPlanId(1L)).thenReturn(List.of());

            assertThat(service.getPlanCourses(1L)).isEmpty();
        }

        @Test
        @DisplayName("addPlanCourse 应按命令构建课程并绑定 planId 保存")
        void shouldAddPlanCourse() {
            CreatePlanCourseCommand cmd = new CreatePlanCourseCommand();
            cmd.setCourseId(900L);
            cmd.setSemesterNumber(2);
            cmd.setCourseCategory(1);
            cmd.setCourseType(1);
            cmd.setCredits(new BigDecimal("3"));
            cmd.setTotalHours(48);
            cmd.setWeeklyHours(3);
            cmd.setTheoryHours(40);
            cmd.setPracticeHours(8);
            cmd.setAssessmentMethod(2);
            cmd.setSortOrder(1);
            cmd.setRemark("新增课程");

            when(planCourseRepository.save(any(PlanCourse.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            PlanCourseDTO dto = service.addPlanCourse(50L, cmd);

            ArgumentCaptor<PlanCourse> cap = ArgumentCaptor.forClass(PlanCourse.class);
            verify(planCourseRepository).save(cap.capture());
            PlanCourse saved = cap.getValue();
            assertThat(saved.getPlanId()).isEqualTo(50L);
            assertThat(saved.getCourseId()).isEqualTo(900L);
            assertThat(saved.getSemesterNumber()).isEqualTo(2);
            assertThat(dto.getCourseId()).isEqualTo(900L);
        }

        @Test
        @DisplayName("updatePlanCourse 存在时应更新字段并保存")
        void shouldUpdatePlanCourse() {
            PlanCourse pc = buildPlanCourse(10L, 1L);
            when(planCourseRepository.findById(10L)).thenReturn(Optional.of(pc));
            when(planCourseRepository.save(any(PlanCourse.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            UpdatePlanCourseCommand cmd = new UpdatePlanCourseCommand();
            cmd.setSemesterNumber(6);
            cmd.setCredits(new BigDecimal("6"));
            cmd.setRemark("已修改");

            service.updatePlanCourse(1L, 10L, cmd);

            assertThat(pc.getSemesterNumber()).isEqualTo(6);
            assertThat(pc.getCredits()).isEqualByComparingTo("6");
            assertThat(pc.getRemark()).isEqualTo("已修改");
            verify(planCourseRepository).save(pc);
        }

        @Test
        @DisplayName("updatePlanCourse 不存在时抛异常且不保存")
        void shouldThrowWhenUpdateMissingPlanCourse() {
            when(planCourseRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePlanCourse(1L, 404L, new UpdatePlanCourseCommand()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("方案课程不存在");
            verify(planCourseRepository, never()).save(any(PlanCourse.class));
        }

        @Test
        @DisplayName("removePlanCourse 应按课程记录ID删除")
        void shouldRemovePlanCourse() {
            service.removePlanCourse(1L, 10L);

            verify(planCourseRepository).deleteById(10L);
            verify(planCourseRepository, never()).findById(anyLong());
        }
    }
}
