package com.school.management.application.student;

import com.school.management.application.student.command.*;
import com.school.management.application.student.query.StudentDTO;
import com.school.management.application.student.query.StudentQueryCriteria;
import com.school.management.common.PageResult;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.domain.student.model.aggregate.Student;
import com.school.management.domain.student.model.valueobject.Gender;
import com.school.management.domain.student.model.valueobject.StudentStatus;
import com.school.management.domain.student.repository.StudentRepository;
import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentApplicationService 测试")
class StudentApplicationServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private StudentApplicationService service;

    // 测试数据工厂方法
    private Student createTestStudent(Long id, String studentNo, String name) {
        return createStudentWithStatus(id, studentNo, name, StudentStatus.STUDYING);
    }

    private Student createSuspendedStudent(Long id, String studentNo, String name) {
        return createStudentWithStatus(id, studentNo, name, StudentStatus.SUSPENDED);
    }

    private Student createStudentWithStatus(Long id, String studentNo, String name, StudentStatus status) {
        return Student.reconstruct(
                id,
                studentNo,
                name,
                Gender.MALE,
                "320123200001011234",
                "13800138000",
                "test@example.com",
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2027, 6, 30),
                1L,  // orgUnitId
                status,
                null,
                "北京市海淀区",
                "张三",
                "13900139000",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("学生入学")
    class EnrollStudentTests {

        @Test
        @DisplayName("应成功办理入学")
        void shouldEnrollSuccessfully() {
            // Given
            EnrollStudentCommand command = EnrollStudentCommand.builder()
                    .studentNo("2024001")
                    .name("张三")
                    .gender(1)
                    .idCard("320123200001011234")
                    .orgUnitId(1L)
                    .enrollmentDate(LocalDate.of(2024, 9, 1))
                    .build();

            when(studentRepository.existsByStudentNo("2024001")).thenReturn(false);
            when(studentRepository.existsByIdCard("320123200001011234")).thenReturn(false);
            when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
                Student s = inv.getArgument(0);
                // 模拟保存后设置 ID
                return Student.reconstruct(
                        1L, s.getStudentNo(), s.getName(), s.getGender(),
                        s.getIdCard(), s.getPhone(), s.getEmail(), s.getBirthDate(),
                        s.getEnrollmentDate(), s.getExpectedGraduationDate(), s.getOrgUnitId(),
                        s.getStatus(), s.getAvatarUrl(),
                        s.getHomeAddress(), s.getEmergencyContact(), s.getEmergencyPhone(),
                        s.getRemark(), LocalDateTime.now(), LocalDateTime.now()
                );
            });

            // When
            Long result = service.enrollStudent(command);

            // Then
            assertThat(result).isEqualTo(1L);
            verify(studentRepository).save(any(Student.class));
            verify(eventPublisher).publishAll(anyList());
        }

        @Test
        @DisplayName("学号重复时应抛出异常")
        void shouldThrowWhenStudentNoExists() {
            // Given
            EnrollStudentCommand command = EnrollStudentCommand.builder()
                    .studentNo("2024001")
                    .name("张三")
                    .build();

            when(studentRepository.existsByStudentNo("2024001")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.enrollStudent(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("学号已存在");

            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("身份证号重复时应抛出异常")
        void shouldThrowWhenIdCardExists() {
            // Given
            EnrollStudentCommand command = EnrollStudentCommand.builder()
                    .studentNo("2024001")
                    .name("张三")
                    .idCard("320123200001011234")
                    .build();

            when(studentRepository.existsByStudentNo("2024001")).thenReturn(false);
            when(studentRepository.existsByIdCard("320123200001011234")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.enrollStudent(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("身份证号已存在");

            verify(studentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("更新学生信息")
    class UpdateStudentTests {

        @Test
        @DisplayName("应成功更新学生信息")
        void shouldUpdateSuccessfully() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            UpdateStudentCommand command = UpdateStudentCommand.builder()
                    .id(1L)
                    .name("李四")
                    .phone("13900139001")
                    .build();

            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
            when(studentRepository.save(any(Student.class))).thenReturn(student);

            // When
            service.updateStudent(command);

            // Then
            verify(studentRepository).save(any(Student.class));
            verify(eventPublisher).publishAll(anyList());
        }

        @Test
        @DisplayName("学生不存在时应抛出异常")
        void shouldThrowWhenStudentNotFound() {
            // Given
            UpdateStudentCommand command = UpdateStudentCommand.builder()
                    .id(999L)
                    .name("李四")
                    .build();

            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.updateStudent(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("学生不存在");

            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("修改学号时检查重复")
        void shouldCheckStudentNoWhenChanging() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            UpdateStudentCommand command = UpdateStudentCommand.builder()
                    .id(1L)
                    .studentNo("2024002")
                    .name("张三")
                    .build();

            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
            when(studentRepository.existsByStudentNo("2024002")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.updateStudent(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("学号已存在");

            verify(studentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("删除学生")
    class DeleteStudentTests {

        @Test
        @DisplayName("应成功删除学生")
        void shouldDeleteSuccessfully() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

            // When
            service.deleteStudent(1L);

            // Then
            verify(studentRepository).delete(student);
        }

        @Test
        @DisplayName("学生不存在时应抛出异常")
        void shouldThrowWhenStudentNotFound() {
            // Given
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.deleteStudent(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("学生不存在");

            verify(studentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("批量删除学生")
        void shouldBatchDeleteStudents() {
            // Given
            Student student1 = createTestStudent(1L, "2024001", "张三");
            Student student2 = createTestStudent(2L, "2024002", "李四");

            when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
            when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            service.deleteStudents(Arrays.asList(1L, 2L, 999L));

            // Then
            verify(studentRepository).delete(student1);
            verify(studentRepository).delete(student2);
            verify(studentRepository, times(2)).delete(any());
        }
    }

    @Nested
    @DisplayName("转班")
    class TransferClassTests {

        @Test
        @DisplayName("应成功转班")
        void shouldTransferClassSuccessfully() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            TransferClassCommand command = TransferClassCommand.builder()
                    .studentId(1L)
                    .newClassId(2L)
                    .build();

            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
            when(studentRepository.save(any(Student.class))).thenReturn(student);

            // When
            service.transferClass(command);

            // Then
            verify(studentRepository).save(any(Student.class));
            verify(eventPublisher).publishAll(anyList());
        }

        @Test
        @DisplayName("学生不存在时应抛出异常")
        void shouldThrowWhenStudentNotFound() {
            // Given
            TransferClassCommand command = TransferClassCommand.builder()
                    .studentId(999L)
                    .newClassId(2L)
                    .build();

            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.transferClass(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("学生不存在");
        }
    }

    // DormitoryTests removed — dormitory management now uses UniversalPlace system

    @Nested
    @DisplayName("学籍状态变更")
    class StatusChangeTests {

        @Test
        @DisplayName("应成功休学")
        void shouldSuspendSuccessfully() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
            when(studentRepository.save(any(Student.class))).thenReturn(student);

            // When
            service.suspend(1L, "因病休学");

            // Then
            verify(studentRepository).save(any(Student.class));
            verify(eventPublisher).publishAll(anyList());
        }

        @Test
        @DisplayName("应成功复学")
        void shouldResumeSuccessfully() {
            // Given - 复学需要休学状态的学生
            Student student = createSuspendedStudent(1L, "2024001", "张三");
            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
            when(studentRepository.save(any(Student.class))).thenReturn(student);

            // When
            service.resume(1L, "恢复学业");

            // Then
            verify(studentRepository).save(any(Student.class));
            verify(eventPublisher).publishAll(anyList());
        }

        @Test
        @DisplayName("应成功毕业")
        void shouldGraduateSuccessfully() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
            when(studentRepository.save(any(Student.class))).thenReturn(student);

            // When
            service.graduate(1L);

            // Then
            verify(studentRepository).save(any(Student.class));
            verify(eventPublisher).publishAll(anyList());
        }

        @Test
        @DisplayName("应成功退学")
        void shouldWithdrawSuccessfully() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
            when(studentRepository.save(any(Student.class))).thenReturn(student);

            // When
            service.withdraw(1L, "自愿退学");

            // Then
            verify(studentRepository).save(any(Student.class));
            verify(eventPublisher).publishAll(anyList());
        }
    }

    @Nested
    @DisplayName("查询学生")
    class QueryTests {

        @Test
        @DisplayName("应根据ID获取学生")
        void shouldGetStudentById() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

            // When
            StudentDTO result = service.getById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStudentNo()).isEqualTo("2024001");
            assertThat(result.getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("学生不存在时应抛出异常")
        void shouldThrowWhenStudentNotFoundById() {
            // Given
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("学生不存在");
        }

        @Test
        @DisplayName("应根据学号获取学生")
        void shouldGetStudentByStudentNo() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            when(studentRepository.findByStudentNo("2024001")).thenReturn(Optional.of(student));

            // When
            StudentDTO result = service.getByStudentNo("2024001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStudentNo()).isEqualTo("2024001");
        }

        @Test
        @DisplayName("应根据班级ID获取学生列表")
        void shouldFindByClassId() {
            // Given
            Student student1 = createTestStudent(1L, "2024001", "张三");
            Student student2 = createTestStudent(2L, "2024002", "李四");
            when(studentRepository.findByClassId(1L)).thenReturn(Arrays.asList(student1, student2));

            // When
            List<StudentDTO> result = service.findByClassId(1L);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("张三");
            assertThat(result.get(1).getName()).isEqualTo("李四");
        }

        // findByDormitoryId test removed — dormitory queries now through UniversalPlace

        @Test
        @DisplayName("应分页查询学生")
        void shouldFindByPage() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            StudentQueryCriteria criteria = new StudentQueryCriteria();
            criteria.setPageNum(1);
            criteria.setPageSize(10);

            when(studentRepository.findByPage(any(), eq(1), eq(10)))
                    .thenReturn(Collections.singletonList(student));
            when(studentRepository.countByCriteria(any())).thenReturn(1L);

            // When
            PageResult<StudentDTO> result = service.findByPage(criteria);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("统计查询")
    class CountTests {

        @Test
        @DisplayName("应统计班级学生数量")
        void shouldCountByClassId() {
            // Given
            when(studentRepository.countByClassId(1L)).thenReturn(30L);

            // When
            long result = service.countByClassId(1L);

            // Then
            assertThat(result).isEqualTo(30L);
        }

        @Test
        @DisplayName("应统计班级在读学生数量")
        void shouldCountActiveByClassId() {
            // Given
            when(studentRepository.countActiveByClassId(1L)).thenReturn(28L);

            // When
            long result = service.countActiveByClassId(1L);

            // Then
            assertThat(result).isEqualTo(28L);
        }

        @Test
        @DisplayName("应检查学号是否存在")
        void shouldCheckStudentNoExists() {
            // Given
            when(studentRepository.existsByStudentNo("2024001")).thenReturn(true);

            // When
            boolean result = service.existsByStudentNo("2024001", null);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应排除自己检查学号是否存在")
        void shouldCheckStudentNoExistsExcludeSelf() {
            // Given
            Student student = createTestStudent(1L, "2024001", "张三");
            when(studentRepository.findByStudentNo("2024001")).thenReturn(Optional.of(student));

            // When
            boolean result = service.existsByStudentNo("2024001", 1L);

            // Then
            assertThat(result).isFalse();
        }
    }
}
