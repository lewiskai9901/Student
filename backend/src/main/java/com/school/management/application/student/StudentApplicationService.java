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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生应用服务
 * 协调领域层完成业务用例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentApplicationService {

    private final StudentRepository studentRepository;
    private final DomainEventPublisher eventPublisher;
    private final StatusChangeRecordService statusChangeRecordService;

    /**
     * 学生入学
     */
    @Transactional
    public Long enrollStudent(EnrollStudentCommand command) {
        // 检查学号是否已存在
        if (command.getStudentNo() != null && studentRepository.existsByStudentNo(command.getStudentNo())) {
            throw new BusinessException("学号已存在: " + command.getStudentNo());
        }

        // 检查身份证号是否已存在
        if (command.getIdCard() != null && studentRepository.existsByIdCard(command.getIdCard())) {
            throw new BusinessException("身份证号已存在: " + command.getIdCard());
        }

        // 创建学生聚合
        Student student = Student.enroll(
                command.getStudentNo(),
                command.getName(),
                command.getGender() != null ? Gender.fromCode(command.getGender()) : null,
                command.getIdCard(),
                command.getClassId(),
                command.getEnrollmentDate()
        );

        // 设置其他属性
        student.updateBasicInfo(
                command.getName(),
                command.getGender() != null ? Gender.fromCode(command.getGender()) : null,
                command.getPhone(),
                command.getEmail(),
                command.getBirthDate(),
                command.getHomeAddress(),
                command.getEmergencyContact(),
                command.getEmergencyPhone(),
                command.getRemark()
        );

        // 分配宿舍
        if (command.getDormitoryId() != null) {
            student.assignDormitory(command.getDormitoryId(), command.getBedNumber());
        }

        // 保存学生
        Student saved = studentRepository.save(student);

        // 发布领域事件
        eventPublisher.publishAll(student.getDomainEvents());
        student.clearDomainEvents();

        // 记录入学异动
        statusChangeRecordService.recordEnrollment(saved.getId(), saved.getStudentNo(), saved.getName());

        log.info("学生入学成功: {}", saved.getId());
        return saved.getId();
    }

    /**
     * 更新学生信息
     */
    @Transactional
    public void updateStudent(UpdateStudentCommand command) {
        Student student = studentRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException("学生不存在: " + command.getId()));

        // 检查学号是否重复（排除自己）
        if (command.getStudentNo() != null && !command.getStudentNo().equals(student.getStudentNo())) {
            if (studentRepository.existsByStudentNo(command.getStudentNo())) {
                throw new BusinessException("学号已存在: " + command.getStudentNo());
            }
        }

        // 更新基本信息
        student.updateBasicInfo(
                command.getName(),
                command.getGender() != null ? Gender.fromCode(command.getGender()) : null,
                command.getPhone(),
                command.getEmail(),
                command.getBirthDate(),
                command.getHomeAddress(),
                command.getEmergencyContact(),
                command.getEmergencyPhone(),
                command.getRemark()
        );

        // 保存学生
        studentRepository.save(student);

        // 发布领域事件
        eventPublisher.publishAll(student.getDomainEvents());
        student.clearDomainEvents();

        log.info("学生信息更新成功: {}", command.getId());
    }

    /**
     * 删除学生
     */
    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学生不存在: " + id));

        studentRepository.delete(student);
        log.info("学生删除成功: {}", id);
    }

    /**
     * 批量删除学生
     */
    @Transactional
    public void deleteStudents(List<Long> ids) {
        for (Long id : ids) {
            Student student = studentRepository.findById(id).orElse(null);
            if (student != null) {
                studentRepository.delete(student);
            }
        }
        log.info("批量删除学生成功, 数量: {}", ids.size());
    }

    /**
     * 转班
     */
    @Transactional
    public void transferClass(TransferClassCommand command) {
        Student student = studentRepository.findById(command.getStudentId())
                .orElseThrow(() -> new BusinessException("学生不存在: " + command.getStudentId()));

        Long oldClassId = student.getClassId();
        student.transferClass(command.getNewClassId());
        studentRepository.save(student);

        eventPublisher.publishAll(student.getDomainEvents());
        student.clearDomainEvents();

        // 记录转班异动
        statusChangeRecordService.recordTransfer(
                command.getStudentId(), oldClassId, command.getNewClassId(), command.getReason());

        log.info("学生转班成功: {} -> {}", command.getStudentId(), command.getNewClassId());
    }

    /**
     * 分配宿舍
     */
    @Transactional
    public void assignDormitory(AssignDormitoryCommand command) {
        Student student = studentRepository.findById(command.getStudentId())
                .orElseThrow(() -> new BusinessException("学生不存在: " + command.getStudentId()));

        student.assignDormitory(command.getDormitoryId(), command.getBedNumber());
        studentRepository.save(student);

        log.info("学生宿舍分配成功: {} -> {} 床位{}",
                command.getStudentId(), command.getDormitoryId(), command.getBedNumber());
    }

    /**
     * 移除宿舍
     */
    @Transactional
    public void removeDormitory(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("学生不存在: " + studentId));

        student.assignDormitory(null, null);
        studentRepository.save(student);

        log.info("学生移除宿舍成功: {}", studentId);
    }

    /**
     * 变更学生状态
     */
    @Transactional
    public void changeStatus(ChangeStudentStatusCommand command) {
        Student student = studentRepository.findById(command.getStudentId())
                .orElseThrow(() -> new BusinessException("学生不存在: " + command.getStudentId()));

        StudentStatus oldStatus = student.getStatus();
        student.changeStatus(command.getNewStatus(), command.getReason());
        studentRepository.save(student);

        eventPublisher.publishAll(student.getDomainEvents());
        student.clearDomainEvents();

        // 记录状态异动
        statusChangeRecordService.recordStatusChange(
                command.getStudentId(),
                command.getNewStatus().name(),
                oldStatus != null ? oldStatus.name() : null,
                command.getNewStatus().name(),
                command.getReason());

        log.info("学生状态变更成功: {} -> {}", command.getStudentId(), command.getNewStatus());
    }

    /**
     * 学生休学
     */
    @Transactional
    public void suspend(Long studentId, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("学生不存在: " + studentId));

        StudentStatus oldStatus = student.getStatus();
        student.suspend(reason);
        studentRepository.save(student);

        eventPublisher.publishAll(student.getDomainEvents());
        student.clearDomainEvents();

        // 记录休学异动
        statusChangeRecordService.recordStatusChange(
                studentId, "SUSPEND",
                oldStatus != null ? oldStatus.name() : null,
                "SUSPENDED", reason);

        log.info("学生休学成功: {}", studentId);
    }

    /**
     * 学生复学
     */
    @Transactional
    public void resume(Long studentId, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("学生不存在: " + studentId));

        StudentStatus oldStatus = student.getStatus();
        student.resume(reason);
        studentRepository.save(student);

        eventPublisher.publishAll(student.getDomainEvents());
        student.clearDomainEvents();

        // 记录复学异动
        statusChangeRecordService.recordStatusChange(
                studentId, "RESUME",
                oldStatus != null ? oldStatus.name() : null,
                "STUDYING", reason);

        log.info("学生复学成功: {}", studentId);
    }

    /**
     * 学生毕业
     */
    @Transactional
    public void graduate(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("学生不存在: " + studentId));

        StudentStatus oldStatus = student.getStatus();
        student.graduate();
        studentRepository.save(student);

        eventPublisher.publishAll(student.getDomainEvents());
        student.clearDomainEvents();

        // 记录毕业异动
        statusChangeRecordService.recordStatusChange(
                studentId, "GRADUATE",
                oldStatus != null ? oldStatus.name() : null,
                "GRADUATED", "正常毕业");

        log.info("学生毕业成功: {}", studentId);
    }

    /**
     * 学生退学
     */
    @Transactional
    public void withdraw(Long studentId, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("学生不存在: " + studentId));

        StudentStatus oldStatus = student.getStatus();
        student.withdraw(reason);
        studentRepository.save(student);

        eventPublisher.publishAll(student.getDomainEvents());
        student.clearDomainEvents();

        // 记录退学异动
        statusChangeRecordService.recordStatusChange(
                studentId, "WITHDRAW",
                oldStatus != null ? oldStatus.name() : null,
                "WITHDRAWN", reason);

        log.info("学生退学成功: {}", studentId);
    }

    // ============ 查询方法 ============

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
        StudentRepository.StudentQueryCriteria repoCriteria = new StudentRepository.StudentQueryCriteria();
        repoCriteria.setKeyword(criteria.getKeyword());
        repoCriteria.setClassId(criteria.getClassId());
        repoCriteria.setOrgUnitId(criteria.getOrgUnitId());
        repoCriteria.setGradeLevel(criteria.getGradeLevel());
        repoCriteria.setDormitoryId(criteria.getDormitoryId());
        if (criteria.getStatus() != null) {
            repoCriteria.setStatus(StudentStatus.fromCode(criteria.getStatus()));
        }

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
    public List<StudentDTO> findByClassId(Long classId) {
        return studentRepository.findByClassId(classId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据宿舍ID获取学生列表
     */
    public List<StudentDTO> findByDormitoryId(Long dormitoryId) {
        return studentRepository.findByDormitoryId(dormitoryId).stream()
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
     * 统计班级学生数量
     */
    public long countByClassId(Long classId) {
        return studentRepository.countByClassId(classId);
    }

    /**
     * 统计班级在读学生数量
     */
    public long countActiveByClassId(Long classId) {
        return studentRepository.countActiveByClassId(classId);
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
                .classId(student.getClassId())
                .dormitoryId(student.getDormitoryId())
                .bedNumber(student.getBedNumber())
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
