package com.school.management.application.organization;

import com.school.management.application.organization.command.CreateGradeCommand;
import com.school.management.application.organization.query.GradeDTO;
import com.school.management.domain.organization.model.Grade;
import com.school.management.domain.organization.repository.GradeRepository;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GradeApplicationService 测试")
class GradeApplicationServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private GradeApplicationService service;

    @Nested
    @DisplayName("创建年级")
    class CreateGradeTests {

        @Test
        @DisplayName("应成功创建年级")
        void shouldCreateGradeSuccessfully() {
            // Given
            CreateGradeCommand command = new CreateGradeCommand();
            command.setGradeCode("G2024");
            command.setGradeName("2024级");
            command.setEnrollmentYear(2024);
            command.setSchoolingYears(3);
            command.setCreatedBy(1L);

            when(gradeRepository.existsByGradeCode("G2024")).thenReturn(false);
            when(gradeRepository.existsByEnrollmentYear(2024)).thenReturn(false);
            when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            GradeDTO result = service.createGrade(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getGradeCode()).isEqualTo("G2024");
            assertThat(result.getGradeName()).isEqualTo("2024级");
            verify(gradeRepository).save(any(Grade.class));
        }

        @Test
        @DisplayName("年级编码重复时应抛出异常")
        void shouldThrowWhenGradeCodeExists() {
            // Given
            CreateGradeCommand command = new CreateGradeCommand();
            command.setGradeCode("G2024");
            command.setGradeName("2024级");
            command.setEnrollmentYear(2024);
            command.setSchoolingYears(3);
            command.setCreatedBy(1L);

            when(gradeRepository.existsByGradeCode("G2024")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.createGrade(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("年级编码已存在");
        }
    }

    @Nested
    @DisplayName("删除年级")
    class DeleteGradeTests {

        @Test
        @DisplayName("无班级时应成功删除")
        void shouldDeleteWhenNoClasses() {
            // Given
            Long gradeId = 1L;
            Grade grade = Grade.create("G2024", "2024级", 2024, 3, 1L);

            when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
            when(schoolClassRepository.countByGradeId(gradeId)).thenReturn(0);

            // When
            service.deleteGrade(gradeId);

            // Then
            verify(gradeRepository).deleteById(gradeId);
        }

        @Test
        @DisplayName("有班级时应阻止删除")
        void shouldPreventDeleteWhenHasClasses() {
            // Given
            Long gradeId = 1L;
            Grade grade = Grade.create("G2024", "2024级", 2024, 3, 1L);

            when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
            when(schoolClassRepository.countByGradeId(gradeId)).thenReturn(5);

            // When & Then
            assertThatThrownBy(() -> service.deleteGrade(gradeId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("5 个班级");

            verify(gradeRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("查询年级")
    class GetGradeTests {

        @Test
        @DisplayName("应返回存在的年级")
        void shouldReturnExistingGrade() {
            // Given
            Long gradeId = 1L;
            Grade grade = Grade.create("G2024", "2024级", 2024, 3, 1L);

            when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));

            // When
            GradeDTO result = service.getGrade(gradeId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getGradeCode()).isEqualTo("G2024");
        }

        @Test
        @DisplayName("年级不存在时应抛出异常")
        void shouldThrowWhenGradeNotFound() {
            // Given
            when(gradeRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getGrade(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("年级不存在");
        }
    }
}
