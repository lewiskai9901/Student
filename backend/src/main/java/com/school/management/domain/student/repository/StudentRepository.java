package com.school.management.domain.student.repository;

import com.school.management.domain.shared.Repository;
import com.school.management.domain.student.model.aggregate.Student;
import com.school.management.domain.student.model.valueobject.StudentStatus;

import java.util.List;
import java.util.Optional;

/**
 * 学生仓储接口
 */
public interface StudentRepository extends Repository<Student, Long> {

    /**
     * 根据学号查询
     */
    Optional<Student> findByStudentNo(String studentNo);

    /**
     * 根据身份证号查询
     */
    Optional<Student> findByIdCard(String idCard);

    /**
     * 根据班级ID查询所有学生
     */
    List<Student> findByClassId(Long classId);

    /**
     * 根据班级ID和状态查询
     */
    List<Student> findByClassIdAndStatus(Long classId, StudentStatus status);

    /**
     * 根据宿舍ID查询所有学生
     */
    List<Student> findByDormitoryId(Long dormitoryId);

    /**
     * 检查学号是否存在
     */
    boolean existsByStudentNo(String studentNo);

    /**
     * 检查身份证号是否存在
     */
    boolean existsByIdCard(String idCard);

    /**
     * 统计班级学生数量
     */
    long countByClassId(Long classId);

    /**
     * 统计班级在读学生数量
     */
    long countActiveByClassId(Long classId);

    /**
     * 分页查询
     */
    List<Student> findByPage(StudentQueryCriteria criteria, int pageNum, int pageSize);

    /**
     * 统计符合条件的学生数量
     */
    long countByCriteria(StudentQueryCriteria criteria);

    /**
     * 查询条件
     */
    class StudentQueryCriteria {
        private String keyword;
        private Long classId;
        private Long orgUnitId;
        private Integer gradeLevel;
        private StudentStatus status;
        private Long dormitoryId;

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public Long getClassId() { return classId; }
        public void setClassId(Long classId) { this.classId = classId; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public Integer getGradeLevel() { return gradeLevel; }
        public void setGradeLevel(Integer gradeLevel) { this.gradeLevel = gradeLevel; }
        public StudentStatus getStatus() { return status; }
        public void setStatus(StudentStatus status) { this.status = status; }
        public Long getDormitoryId() { return dormitoryId; }
        public void setDormitoryId(Long dormitoryId) { this.dormitoryId = dormitoryId; }
    }
}
