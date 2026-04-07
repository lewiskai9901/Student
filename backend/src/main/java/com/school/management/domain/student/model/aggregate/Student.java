package com.school.management.domain.student.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.student.event.StudentEnrolledEvent;
import com.school.management.domain.student.event.StudentStatusChangedEvent;
import com.school.management.domain.student.event.StudentUpdatedEvent;
import com.school.management.domain.student.model.valueobject.Gender;
import com.school.management.domain.student.model.valueobject.StudentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生聚合根
 */
public class Student extends AggregateRoot<Long> {

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别
     */
    private Gender gender;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 入学日期
     */
    private LocalDate enrollmentDate;

    /**
     * 预计毕业日期
     */
    private LocalDate expectedGraduationDate;

    /**
     * 班级ID
     */
    private Long orgUnitId;

    /**
     * 学籍状态
     */
    private StudentStatus status;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 家庭住址
     */
    private String homeAddress;

    /**
     * 紧急联系人
     */
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    private String emergencyPhone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // 私有构造函数，确保通过工厂方法创建
    private Student() {}

    /**
     * 创建新学生（入学）
     */
    public static Student enroll(
            String studentNo,
            String name,
            Gender gender,
            String idCard,
            Long orgUnitId,
            LocalDate enrollmentDate
    ) {
        Student student = new Student();
        student.studentNo = studentNo;
        student.name = name;
        student.gender = gender;
        student.idCard = idCard;
        student.orgUnitId = orgUnitId;
        student.enrollmentDate = enrollmentDate;
        student.status = StudentStatus.STUDYING;
        student.createdAt = LocalDateTime.now();
        student.updatedAt = LocalDateTime.now();

        // 发布入学事件
        student.registerEvent(new StudentEnrolledEvent(
                student.getId() != null ? student.getId().toString() : "NEW",
                studentNo,
                name,
                classId
        ));

        return student;
    }

    /**
     * 重建学生（从持久化层恢复）
     */
    public static Student reconstruct(
            Long id,
            String studentNo,
            String name,
            Gender gender,
            String idCard,
            String phone,
            String email,
            LocalDate birthDate,
            LocalDate enrollmentDate,
            LocalDate expectedGraduationDate,
            Long orgUnitId,
            StudentStatus status,
            String avatarUrl,
            String homeAddress,
            String emergencyContact,
            String emergencyPhone,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Student student = new Student();
        student.setId(id);
        student.studentNo = studentNo;
        student.name = name;
        student.gender = gender;
        student.idCard = idCard;
        student.phone = phone;
        student.email = email;
        student.birthDate = birthDate;
        student.enrollmentDate = enrollmentDate;
        student.expectedGraduationDate = expectedGraduationDate;
        student.orgUnitId = orgUnitId;
        student.status = status;
        student.avatarUrl = avatarUrl;
        student.homeAddress = homeAddress;
        student.emergencyContact = emergencyContact;
        student.emergencyPhone = emergencyPhone;
        student.remark = remark;
        student.createdAt = createdAt;
        student.updatedAt = updatedAt;
        return student;
    }

    /**
     * 更新基本信息
     */
    public void updateBasicInfo(
            String name,
            Gender gender,
            String phone,
            String email,
            LocalDate birthDate,
            String homeAddress,
            String emergencyContact,
            String emergencyPhone,
            String remark
    ) {
        this.name = name;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
        this.homeAddress = homeAddress;
        this.emergencyContact = emergencyContact;
        this.emergencyPhone = emergencyPhone;
        this.remark = remark;
        this.updatedAt = LocalDateTime.now();

        // 只有在已持久化（有ID）时才注册更新事件
        if (this.getId() != null) {
            registerEvent(new StudentUpdatedEvent(
                    this.getId().toString(),
                    this.studentNo,
                    this.name
            ));
        }
    }

    /**
     * 转班
     */
    public void transferClass(Long newClassId) {
        if (this.status != StudentStatus.STUDYING) {
            throw new IllegalStateException("只有在读学生可以转班");
        }
        Long oldClassId = this.orgUnitId;
        this.orgUnitId = newClassId;
        this.updatedAt = LocalDateTime.now();

        // 可以发布转班事件
    }

    /**
     * 变更学籍状态
     */
    public void changeStatus(StudentStatus newStatus, String reason) {
        if (this.status.isFinal()) {
            throw new IllegalStateException("学籍状态已为终态，不可变更");
        }

        StudentStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();

        if (this.getId() != null) {
            registerEvent(new StudentStatusChangedEvent(
                    this.getId().toString(),
                    this.studentNo,
                    oldStatus,
                    newStatus,
                    reason
            ));
        }
    }

    /**
     * 休学
     */
    public void suspend(String reason) {
        changeStatus(StudentStatus.SUSPENDED, reason);
    }

    /**
     * 复学
     */
    public void resume(String reason) {
        if (this.status != StudentStatus.SUSPENDED) {
            throw new IllegalStateException("只有休学状态可以复学");
        }
        changeStatus(StudentStatus.STUDYING, reason);
    }

    /**
     * 毕业
     */
    public void graduate() {
        if (this.status != StudentStatus.STUDYING) {
            throw new IllegalStateException("只有在读学生可以毕业");
        }
        changeStatus(StudentStatus.GRADUATED, "正常毕业");
    }

    /**
     * 退学
     */
    public void withdraw(String reason) {
        changeStatus(StudentStatus.WITHDRAWN, reason);
    }

    /**
     * 开除
     */
    public void expel(String reason) {
        changeStatus(StudentStatus.EXPELLED, reason);
    }

    // Getters
    public String getStudentNo() { return studentNo; }
    public String getName() { return name; }
    public Gender getGender() { return gender; }
    public String getIdCard() { return idCard; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDate getBirthDate() { return birthDate; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public LocalDate getExpectedGraduationDate() { return expectedGraduationDate; }
    public Long getOrgUnitId() { return orgUnitId; }
    public StudentStatus getStatus() { return status; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getHomeAddress() { return homeAddress; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters for reconstruction
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void setExpectedGraduationDate(LocalDate expectedGraduationDate) {
        this.expectedGraduationDate = expectedGraduationDate;
        this.updatedAt = LocalDateTime.now();
    }
}
