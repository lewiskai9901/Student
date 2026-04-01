package com.school.management.infrastructure.persistence.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.student.model.aggregate.Student;
import com.school.management.domain.student.model.valueobject.Gender;
import com.school.management.domain.student.model.valueobject.StudentStatus;
import com.school.management.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 学生仓储实现
 */
@Repository
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentRepository {

    private final DddStudentMapper studentMapper;

    @Override
    public Student save(Student student) {
        StudentPO po = toPO(student);
        if (po.getId() == null) {
            studentMapper.insert(po);
        } else {
            studentMapper.updateById(po);
        }
        student.setId(po.getId());
        return student;
    }

    @Override
    public Optional<Student> findById(Long id) {
        // 使用带用户信息的查询
        StudentPO po = studentMapper.selectByIdWithUser(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void delete(Student student) {
        studentMapper.deleteById(student.getId());
    }

    @Override
    public Optional<Student> findByStudentNo(String studentNo) {
        StudentPO po = studentMapper.selectByStudentNo(studentNo);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Student> findByIdCard(String idCard) {
        StudentPO po = studentMapper.selectByIdCard(idCard);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Student> findByClassId(Long classId) {
        return studentMapper.selectByClassId(classId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findByClassIdAndStatus(Long classId, StudentStatus status) {
        return studentMapper.selectByClassIdAndStatus(classId, status.getCode()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByStudentNo(String studentNo) {
        return studentMapper.countByStudentNo(studentNo) > 0;
    }

    @Override
    public boolean existsByIdCard(String idCard) {
        return studentMapper.countByIdCard(idCard) > 0;
    }

    @Override
    public long countByClassId(Long classId) {
        return studentMapper.countByClassId(classId);
    }

    @Override
    public long countActiveByClassId(Long classId) {
        return studentMapper.countActiveByClassId(classId);
    }

    @Override
    public long countByClassIdAndGender(Long classId, Gender gender) {
        return studentMapper.countByClassIdAndGender(classId, gender.getCode());
    }

    @Override
    public List<Student> findByPage(StudentQueryCriteria criteria, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;

        List<StudentPO> records;
        if (StringUtils.hasText(criteria.getKeyword())) {
            // 使用关键字搜索
            records = studentMapper.selectPageByKeyword(criteria.getKeyword(), offset, pageSize);
        } else {
            // 使用通用分页查询
            records = studentMapper.selectPageWithUser(offset, pageSize);
        }

        return records.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCriteria(StudentQueryCriteria criteria) {
        if (StringUtils.hasText(criteria.getKeyword())) {
            return studentMapper.countByKeyword(criteria.getKeyword());
        }
        return studentMapper.countAll();
    }

    private StudentPO toPO(Student student) {
        StudentPO po = new StudentPO();
        po.setId(student.getId());
        po.setStudentNo(student.getStudentNo());
        po.setName(student.getName());
        po.setGender(student.getGender() != null ? student.getGender().getCode() : null);
        po.setIdCard(student.getIdCard());
        po.setPhone(student.getPhone());
        po.setEmail(student.getEmail());
        po.setBirthDate(student.getBirthDate());
        po.setEnrollmentDate(student.getEnrollmentDate());
        po.setExpectedGraduationDate(student.getExpectedGraduationDate());
        po.setClassId(student.getClassId());
        po.setStatus(student.getStatus() != null ? student.getStatus().getCode() : null);
        po.setAvatarUrl(student.getAvatarUrl());
        po.setHomeAddress(student.getHomeAddress());
        po.setEmergencyContact(student.getEmergencyContact());
        po.setEmergencyPhone(student.getEmergencyPhone());
        po.setRemark(student.getRemark());
        po.setCreatedAt(student.getCreatedAt());
        po.setUpdatedAt(student.getUpdatedAt());
        return po;
    }

    private Student toDomain(StudentPO po) {
        return Student.reconstruct(
                po.getId(),
                po.getStudentNo(),
                po.getName(),
                po.getGender() != null ? Gender.fromCode(po.getGender()) : null,
                po.getIdCard(),
                po.getPhone(),
                po.getEmail(),
                po.getBirthDate(),
                po.getEnrollmentDate(),
                po.getExpectedGraduationDate(),
                po.getClassId(),
                po.getStatus() != null ? StudentStatus.fromCode(po.getStatus()) : null,
                po.getAvatarUrl(),
                po.getHomeAddress(),
                po.getEmergencyContact(),
                po.getEmergencyPhone(),
                po.getRemark(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
