package com.school.management.application.student;

import com.school.management.application.student.query.StudentDTO;
import com.school.management.application.student.query.StudentQueryCriteria;
import com.school.management.common.PageResult;
import com.school.management.domain.student.model.aggregate.Student;
import com.school.management.domain.student.model.valueobject.StudentStatus;
import com.school.management.domain.student.repository.StudentRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生查询服务 - CQRS查询端
 * 专门处理只读查询操作，不修改任何状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentQueryService {

    /**
     * Maximum page size for unpaginated list queries to avoid loading
     * excessive data into memory (e.g., Integer.MAX_VALUE).
     */
    private static final int MAX_UNPAGINATED_SIZE = 10000;

    private final StudentRepository studentRepository;

    /**
     * 根据ID获取学生
     */
    public StudentDTO getById(Long id) {
        return studentRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("学生不存在: " + id));
    }

    /**
     * 根据学号获取学生
     */
    public StudentDTO getByStudentNo(String studentNo) {
        return studentRepository.findByStudentNo(studentNo)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("学生不存在: " + studentNo));
    }

    /**
     * 分页查询学生
     */
    public PageResult<StudentDTO> findByPage(StudentQueryCriteria criteria) {
        StudentRepository.StudentQueryCriteria repoCriteria = buildRepositoryCriteria(criteria);

        int pageNum = criteria.getPageNum() != null ? criteria.getPageNum() : 1;
        int pageSize = criteria.getPageSize() != null ? criteria.getPageSize() : 10;

        List<Student> students = studentRepository.findByPage(repoCriteria, pageNum, pageSize);
        long total = studentRepository.countByCriteria(repoCriteria);

        List<StudentDTO> dtos = students.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtos, total, pageNum, pageSize);
    }

    /**
     * 根据班级ID获取学生列表
     */
    public List<StudentDTO> findByClassId(Long orgUnitId) {
        return studentRepository.findByClassId(orgUnitId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据组织单元ID获取学生列表
     */
    public List<StudentDTO> findByOrgUnitId(Long orgUnitId) {
        StudentRepository.StudentQueryCriteria criteria = new StudentRepository.StudentQueryCriteria();
        criteria.setOrgUnitId(orgUnitId);
        return studentRepository.findByPage(criteria, 1, MAX_UNPAGINATED_SIZE).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据状态查询学生
     */
    public List<StudentDTO> findByStatus(StudentStatus status) {
        StudentRepository.StudentQueryCriteria criteria = new StudentRepository.StudentQueryCriteria();
        criteria.setStatus(status);
        return studentRepository.findByPage(criteria, 1, MAX_UNPAGINATED_SIZE).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查学号是否存在
     */
    public boolean existsByStudentNo(String studentNo, Long excludeId) {
        if (excludeId != null) {
            return studentRepository.findByStudentNo(studentNo)
                    .map(s -> !s.getId().equals(excludeId))
                    .orElse(false);
        }
        return studentRepository.existsByStudentNo(studentNo);
    }

    /**
     * 检查身份证号是否存在
     */
    public boolean existsByIdCard(String idCard, Long excludeId) {
        if (excludeId != null) {
            return studentRepository.findByIdCard(idCard)
                    .map(s -> !s.getId().equals(excludeId))
                    .orElse(false);
        }
        return studentRepository.existsByIdCard(idCard);
    }

    /**
     * 统计班级学生数量
     */
    public long countByClassId(Long orgUnitId) {
        return studentRepository.countByClassId(orgUnitId);
    }

    /**
     * 统计班级在读学生数量
     */
    public long countActiveByClassId(Long orgUnitId) {
        return studentRepository.countActiveByClassId(orgUnitId);
    }

    /**
     * 按状态统计学生数量
     */
    public long countByStatus(StudentStatus status) {
        StudentRepository.StudentQueryCriteria criteria = new StudentRepository.StudentQueryCriteria();
        criteria.setStatus(status);
        return studentRepository.countByCriteria(criteria);
    }

    /**
     * 构建仓储查询条件
     */
    private StudentRepository.StudentQueryCriteria buildRepositoryCriteria(StudentQueryCriteria criteria) {
        StudentRepository.StudentQueryCriteria repoCriteria = new StudentRepository.StudentQueryCriteria();
        repoCriteria.setKeyword(criteria.getKeyword());
        repoCriteria.setOrgUnitId(criteria.getOrgUnitId());
        repoCriteria.setOrgUnitId(criteria.getOrgUnitId());
        repoCriteria.setGradeLevel(criteria.getGradeLevel());
        if (criteria.getStatus() != null) {
            repoCriteria.setStatus(StudentStatus.fromCode(criteria.getStatus()));
        }
        return repoCriteria;
    }

    /**
     * 转换为DTO
     */
    private StudentDTO toDTO(Student student) {
        return StudentDTO.builder()
                .id(student.getId())
                .studentNo(student.getStudentNo())
                .name(student.getName())
                .gender(student.getGender() != null ? student.getGender().getCode() : null)
                .genderText(student.getGender() != null ? student.getGender().getName() : null)
                .idCard(student.getIdCard())
                .phone(student.getPhone())
                .email(student.getEmail())
                .birthDate(student.getBirthDate())
                .enrollmentDate(student.getEnrollmentDate())
                .expectedGraduationDate(student.getExpectedGraduationDate())
                .orgUnitId(student.getOrgUnitId())
                .status(student.getStatus() != null ? student.getStatus().getCode() : null)
                .statusText(student.getStatus() != null ? student.getStatus().getName() : null)
                .avatarUrl(student.getAvatarUrl())
                .homeAddress(student.getHomeAddress())
                .emergencyContact(student.getEmergencyContact())
                .emergencyPhone(student.getEmergencyPhone())
                .remark(student.getRemark())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
