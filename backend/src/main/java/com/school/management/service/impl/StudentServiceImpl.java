package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.common.PageResult;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.StudentCreateRequest;
import com.school.management.dto.StudentQueryRequest;
import com.school.management.dto.StudentResponse;
import com.school.management.dto.StudentUpdateRequest;
import com.school.management.entity.Student;
import com.school.management.entity.User;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.StudentMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.UserRoleMapper;
import com.school.management.service.StudentService;
import com.school.management.annotation.DataPermission;
import com.school.management.util.ExcelUtils;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ClassMapper classMapper;
    private final UserRoleMapper userRoleMapper;
    private final com.school.management.service.DataPermissionService dataPermissionService;

    /** 学生账号默认密码 */
    @Value("${app.security.default-student-password:Stu@123456}")
    private String defaultStudentPassword;

    /** 班主任角色ID */
    private static final Long CLASS_TEACHER_ROLE_ID = 5L;
    /** 学生模块编码 */
    private static final String MODULE_STUDENT = "student";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStudent(StudentCreateRequest request) {
        // 自动生成学号（如果未提供）
        String studentNo = request.getStudentNo();
        if (studentNo == null || studentNo.trim().isEmpty()) {
            studentNo = generateStudentNo();
        }
        log.info("创建学生: {}", studentNo);

        // 检查学号是否存在
        if (existsStudentNo(studentNo, null)) {
            throw new BusinessException(ResultCode.STUDENT_NO_EXISTS);
        }

        // 自动生成用户名（如果未提供，默认使用学号）
        String username = request.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = studentNo;
        }

        // 检查用户名是否存在
        if (existsUsername(username, null)) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        // 自动设置默认密码（如果未提供）
        String password = request.getPassword();
        if (password == null || password.trim().isEmpty()) {
            password = defaultStudentPassword;
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setStatus(1);

        userMapper.insert(user);

        // 更新request中的学号，供后续使用
        request.setStudentNo(studentNo);

        // 创建学生
        Student student = new Student();
        student.setStudentNo(request.getStudentNo());
        student.setIdCardType(request.getIdCardType());
        student.setEthnicity(request.getEthnicity());
        student.setPoliticalStatus(request.getPoliticalStatus());
        student.setRealName(request.getRealName());
        student.setGender(request.getGender());
        student.setPhone(request.getPhone());
        student.setIdCard(request.getIdCard());
        student.setGraduatedSchool(request.getGraduatedSchool());
        student.setHomeAddress(request.getHomeAddress());
        student.setHukouProvince(request.getHukouProvince());
        student.setHukouCity(request.getHukouCity());
        student.setHukouDistrict(request.getHukouDistrict());
        student.setHukouAddress(request.getHukouAddress());
        student.setHukouType(request.getHukouType());
        student.setPostalCode(request.getPostalCode());
        student.setIsPovertyRegistered(request.getIsPovertyRegistered());
        student.setFinancialAidType(request.getFinancialAidType());
        student.setUserId(user.getId());
        student.setClassId(request.getClassId());
        student.setGradeId(request.getGradeId());
        student.setMajorId(request.getMajorId());
        student.setMajorDirectionId(request.getMajorDirectionId());
        student.setEducationLevel(request.getEducationLevel());
        student.setStudyLength(request.getStudyLength());
        student.setDegreeType(request.getDegreeType());
        student.setAdmissionDate(request.getAdmissionDate());
        student.setGraduationDate(request.getGraduationDate());
        student.setEntryLevel(request.getEntryLevel());
        student.setEducationSystem(request.getEducationSystem());
        student.setStudentStatus(request.getStudentStatus() != null ? request.getStudentStatus() : 0);
        student.setDormitoryId(request.getDormitoryId());
        student.setBedNumber(request.getBedNumber());
        student.setGuardianName(request.getGuardianName());
        student.setGuardianPhone(request.getGuardianPhone());
        student.setGuardianRelation(request.getGuardianRelation());
        student.setFatherName(request.getFatherName());
        student.setFatherIdCard(request.getFatherIdCard());
        student.setFatherPhone(request.getFatherPhone());
        student.setMotherName(request.getMotherName());
        student.setMotherIdCard(request.getMotherIdCard());
        student.setMotherPhone(request.getMotherPhone());
        student.setGuardianIdCard(request.getGuardianIdCard());

        studentMapper.insert(student);

        log.info("学生创建成功: {} - {}", student.getId(), request.getStudentNo());
        return student.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStudent(StudentUpdateRequest request) {
        log.info("更新学生信息: {}", request.getId());

        // 数据权限校验
        if (!dataPermissionService.hasAllDataPermission(MODULE_STUDENT)) {
            if (!dataPermissionService.canAccessStudent(request.getId(), MODULE_STUDENT)) {
                log.warn("【数据权限】用户无权修改学生ID: {}", request.getId());
                throw new BusinessException(ResultCode.FORBIDDEN, "无权修改该学生信息");
            }
        }

        Student student = studentMapper.selectById(request.getId());
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }

        // 检查学号是否重复
        if (!student.getStudentNo().equals(request.getStudentNo())
                && existsStudentNo(request.getStudentNo(), request.getId())) {
            throw new BusinessException(ResultCode.STUDENT_NO_EXISTS);
        }

        // 更新用户信息
        User user = userMapper.selectById(student.getUserId());
        if (user != null) {
            user.setRealName(request.getRealName());
            user.setPhone(request.getPhone());
            user.setGender(request.getGender());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        // 更新学生信息
        student.setStudentNo(request.getStudentNo());
        student.setIdCardType(request.getIdCardType());
        student.setEthnicity(request.getEthnicity());
        student.setPoliticalStatus(request.getPoliticalStatus());
        student.setRealName(request.getRealName());
        student.setGender(request.getGender());
        student.setPhone(request.getPhone());
        student.setIdCard(request.getIdCard());
        student.setGraduatedSchool(request.getGraduatedSchool());
        student.setHomeAddress(request.getHomeAddress());
        student.setHukouProvince(request.getHukouProvince());
        student.setHukouCity(request.getHukouCity());
        student.setHukouDistrict(request.getHukouDistrict());
        student.setHukouAddress(request.getHukouAddress());
        student.setHukouType(request.getHukouType());
        student.setPostalCode(request.getPostalCode());
        student.setIsPovertyRegistered(request.getIsPovertyRegistered());
        student.setFinancialAidType(request.getFinancialAidType());
        student.setClassId(request.getClassId());
        student.setGradeId(request.getGradeId());
        student.setMajorId(request.getMajorId());
        student.setMajorDirectionId(request.getMajorDirectionId());
        student.setEducationLevel(request.getEducationLevel());
        student.setStudyLength(request.getStudyLength());
        student.setDegreeType(request.getDegreeType());
        student.setAdmissionDate(request.getAdmissionDate());
        student.setGraduationDate(request.getGraduationDate());
        student.setEntryLevel(request.getEntryLevel());
        student.setEducationSystem(request.getEducationSystem());
        student.setStudentStatus(request.getStudentStatus());
        student.setDormitoryId(request.getDormitoryId());
        student.setBedNumber(request.getBedNumber());
        student.setGuardianName(request.getGuardianName());
        student.setGuardianPhone(request.getGuardianPhone());
        student.setGuardianRelation(request.getGuardianRelation());
        student.setFatherName(request.getFatherName());
        student.setFatherIdCard(request.getFatherIdCard());
        student.setFatherPhone(request.getFatherPhone());
        student.setMotherName(request.getMotherName());
        student.setMotherIdCard(request.getMotherIdCard());
        student.setMotherPhone(request.getMotherPhone());
        student.setGuardianIdCard(request.getGuardianIdCard());
        student.setUpdatedAt(LocalDateTime.now());

        int result = studentMapper.updateById(student);
        if (result == 0) {
            throw new BusinessException(ResultCode.DATA_OPERATION_ERROR, "更新学生信息失败");
        }

        log.info("学生信息更新成功: {}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStudent(Long id) {
        log.info("删除学生: {}", id);

        // 数据权限校验
        if (!dataPermissionService.hasAllDataPermission(MODULE_STUDENT)) {
            if (!dataPermissionService.canAccessStudent(id, MODULE_STUDENT)) {
                log.warn("【数据权限】用户无权删除学生ID: {}", id);
                throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该学生信息");
            }
        }

        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }

        // 使用MyBatis-Plus的逻辑删除
        studentMapper.deleteById(id);

        // 逻辑删除关联用户
        if (student.getUserId() != null) {
            userMapper.deleteById(student.getUserId());
        }

        log.info("学生删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStudents(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        log.info("批量删除学生: {}", ids);

        // 批量查询学生获取关联的用户ID（1次查询代替N次）
        List<Student> students = studentMapper.selectBatchIds(ids);
        if (students.isEmpty()) {
            return;
        }

        // 收集关联的用户ID
        List<Long> userIds = students.stream()
                .map(Student::getUserId)
                .filter(userId -> userId != null)
                .toList();

        // 批量删除学生（1次查询代替N次）
        studentMapper.deleteBatchIds(ids);

        // 批量删除关联用户（1次查询代替N次）
        if (!userIds.isEmpty()) {
            userMapper.deleteBatchIds(userIds);
        }

        log.info("批量删除学生成功，学生数: {}，关联用户数: {}", ids.size(), userIds.size());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        // 数据权限校验：检查当前用户是否有权访问该学生
        if (!dataPermissionService.hasAllDataPermission(MODULE_STUDENT)) {
            if (!dataPermissionService.canAccessStudent(id, MODULE_STUDENT)) {
                log.warn("【数据权限】用户无权访问学生ID: {}", id);
                throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该学生信息");
            }
        }
        return studentMapper.selectStudentById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentByNo(String studentNo) {
        return studentMapper.selectStudentByNo(studentNo);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentByUserId(Long userId) {
        return studentMapper.selectStudentByUserId(userId);
    }

    @Override
    @DataPermission(module = "student", classField = "classIds")
    @Transactional(readOnly = true)
    public PageResult<StudentResponse> getStudentPage(StudentQueryRequest request) {
        // 数据权限过滤由AOP自动处理
        Page<StudentResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<StudentResponse> result = studentMapper.selectStudentPage(page, request);
        return PageResult.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsStudentNo(String studentNo, Long excludeId) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getStudentNo, studentNo);
        wrapper.eq(Student::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Student::getId, excludeId);
        }
        return studentMapper.selectCount(wrapper) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> searchStudents(String keyword, Long classId, Integer limit) {
        log.info("快速搜索学生: keyword={}, classId={}, limit={}", keyword, classId, limit);

        // 使用简单搜索方法（不使用分页，直接LIMIT）
        return studentMapper.searchStudentsSimple(keyword, classId, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStudentStatus(Long id, Integer status) {
        log.info("更新学生状态: {} -> {}", id, status);

        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }

        student.setStudentStatus(status);
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.updateById(student);

        log.info("学生状态更新成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDormitory(Long studentId, Long dormitoryId, String bedNumber) {
        log.info("分配宿舍: {} -> {} - {}", studentId, dormitoryId, bedNumber);

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }

        student.setDormitoryId(dormitoryId);
        student.setBedNumber(bedNumber);
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.updateById(student);

        log.info("宿舍分配成功: {}", studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDormitory(Long studentId) {
        log.info("从宿舍移除学生: {}", studentId);

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }

        log.info("当前学生宿舍信息: dormitoryId={}, bedNumber={}",
            student.getDormitoryId(), student.getBedNumber());

        // 使用 UpdateWrapper 明确设置 NULL 值
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Student> updateWrapper =
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(Student::getId, studentId)
                    .set(Student::getDormitoryId, null)
                    .set(Student::getBedNumber, null)
                    .set(Student::getUpdatedAt, LocalDateTime.now());

        int updateResult = studentMapper.update(null, updateWrapper);
        log.info("学生宿舍信息更新结果: updateResult={}", updateResult);

        if (updateResult == 0) {
            throw new BusinessException(ResultCode.DATA_OPERATION_ERROR, "更新学生宿舍信息失败");
        }

        log.info("学生宿舍移除成功: {}", studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferClass(Long studentId, Long newClassId) {
        log.info("学生转班: {} -> {}", studentId, newClassId);

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }

        // 更新学生班级
        student.setClassId(newClassId);
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.updateById(student);

        // 更新用户班级
        User user = userMapper.selectById(student.getUserId());
        if (user != null) {
            user.setClassId(newClassId);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        log.info("学生转班成功: {}", studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long studentId, String newPassword) {
        log.info("重置学生密码: {}", studentId);

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }

        User user = userMapper.selectById(student.getUserId());
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChangedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        log.info("学生密码重置成功: {}", studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer countStudentsByClassId(Long classId) {
        return studentMapper.countStudentsByClassId(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer countStudentsByDormitoryId(Long dormitoryId) {
        return studentMapper.countStudentsByDormitoryId(dormitoryId);
    }

    /**
     * 检查用户名是否存在
     */
    @Transactional(readOnly = true)
    private boolean existsUsername(String username, Long excludeId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        wrapper.eq(User::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(User::getId, excludeId);
        }
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public byte[] exportStudents(StudentQueryRequest request) throws IOException {
        // 查询所有符合条件的学生数据
        request.setPageNum(1);
        request.setPageSize(10000); // 设置一个较大的值来获取所有数据
        PageResult<StudentResponse> pageResult = getStudentPage(request);
        List<StudentResponse> students = pageResult.getRecords();

        // 准备Excel数据 - 按用户提供的Excel模板格式
        String[] headers = {
            "序号", "姓名", "学号", "证件类型", "身份证号", "出生年月", "性别", "民族", "政治面貌", "联系方式",
            "招生年度", "所在系", "所学专业", "专业代码", "班级", "班主任", "专业级别（层次）", "学制", "学历",
            "户口所在地-省", "户口所在地-市", "户口所在地-区", "户口-地址", "户口性质", "邮政编码",
            "是否建档立卡", "资助申请类型", "父亲姓名", "父亲身份证号", "母亲姓名", "母亲身份证号",
            "其他监护人姓名", "其他监护人身份证号", "状态", "备注"
        };

        List<Object[]> data = new ArrayList<>();
        int index = 1;
        for (StudentResponse student : students) {
            // 从入学日期提取招生年度
            String admissionYear = "";
            if (student.getAdmissionDate() != null) {
                admissionYear = String.valueOf(student.getAdmissionDate().getYear());
            }

            Object[] row = new Object[]{
                index++,
                student.getRealName(),
                student.getStudentNo(),
                student.getIdCardType() != null ? student.getIdCardType() : "身份证",
                student.getIdCard(),
                student.getBirthDate(),
                student.getGender() != null ? (student.getGender() == 1 ? "男" : "女") : "",
                student.getEthnicity(),
                student.getPoliticalStatus(),
                student.getPhone(),
                admissionYear,
                "", // 所在系 - 暂无
                student.getMajorName(),
                "", // 专业代码 - 暂无
                student.getClassName(),
                "", // 班主任 - 暂无
                student.getEducationLevel(),
                student.getStudyLength(),
                student.getDegreeType(),
                student.getHukouProvince(),
                student.getHukouCity(),
                student.getHukouDistrict(),
                student.getHukouAddress(),
                student.getHukouType(),
                student.getPostalCode(),
                student.getIsPovertyRegistered() != null && student.getIsPovertyRegistered() == 1 ? "是" : "否",
                student.getFinancialAidType(),
                student.getFatherName(),
                student.getFatherIdCard(),
                student.getMotherName(),
                student.getMotherIdCard(),
                student.getGuardianName(),
                student.getGuardianIdCard(),
                getStatusName(student.getStudentStatus()),
                student.getRemark()
            };
            data.add(row);
        }

        return ExcelUtils.exportExcel(headers, data, "学生信息");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importStudents(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        List<List<Object>> data = ExcelUtils.importExcel(file.getInputStream(), 1);

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        // 预先加载所有班级，用于名称转ID
        List<com.school.management.entity.Class> allClasses = classMapper.selectList(
            new LambdaQueryWrapper<com.school.management.entity.Class>()
                .eq(com.school.management.entity.Class::getDeleted, 0)
        );
        java.util.Map<String, Long> classNameToId = allClasses.stream()
            .collect(java.util.stream.Collectors.toMap(
                com.school.management.entity.Class::getClassName,
                com.school.management.entity.Class::getId,
                (v1, v2) -> v1
            ));

        for (int i = 0; i < data.size(); i++) {
            List<Object> row = data.get(i);
            // 新模板格式：学号、姓名、性别、班级、手机号、身份证号
            if (row.size() < 4) { // 至少需要学号、姓名、性别、班级
                failCount++;
                errorMsg.append(String.format("第%d行: 数据不完整，至少需要学号、姓名、性别、班级\n", i + 2));
                continue;
            }

            try {
                String studentNo = row.get(0) != null ? row.get(0).toString().trim() : "";
                String realName = row.get(1) != null ? row.get(1).toString().trim() : "";
                String genderStr = row.get(2) != null ? row.get(2).toString().trim() : "";
                String className = row.get(3) != null ? row.get(3).toString().trim() : "";

                // 校验必填字段
                if (studentNo.isEmpty()) {
                    throw new BusinessException("学号不能为空");
                }
                if (realName.isEmpty()) {
                    throw new BusinessException("姓名不能为空");
                }
                if (genderStr.isEmpty()) {
                    throw new BusinessException("性别不能为空");
                }
                if (className.isEmpty()) {
                    throw new BusinessException("班级不能为空");
                }

                // 查找班级ID
                Long classId = classNameToId.get(className);
                if (classId == null) {
                    throw new BusinessException("班级\"" + className + "\"不存在");
                }

                StudentCreateRequest request = new StudentCreateRequest();
                request.setStudentNo(studentNo);
                request.setRealName(realName);
                request.setGender("男".equals(genderStr) ? 1 : 2);
                request.setClassId(classId);

                // 默认用户名和密码
                request.setUsername(studentNo);
                request.setPassword("123456");

                // 可选字段
                if (row.size() > 4 && row.get(4) != null) {
                    String phone = row.get(4).toString().trim();
                    if (!phone.isEmpty()) {
                        request.setPhone(phone);
                    }
                }
                if (row.size() > 5 && row.get(5) != null) {
                    String idCard = row.get(5).toString().trim();
                    if (!idCard.isEmpty()) {
                        request.setIdCard(idCard);
                    }
                }

                createStudent(request);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errorMsg.append(String.format("第%d行: %s\n", i + 2, e.getMessage()));
            }
        }

        return String.format("导入完成! 成功: %d条, 失败: %d条\n%s",
                successCount, failCount, errorMsg.toString());
    }

    @Override
    public byte[] getImportTemplate() throws IOException {
        // 完整模板 - 按用户提供的Excel格式
        String[] headers = {
            "序号", "*姓名", "学号", "证件类型", "*身份证号", "*出生年月", "*性别", "*民族", "*政治面貌", "*联系方式",
            "*招生年度", "*所在系", "所学专业", "专业代码", "班级", "*班主任",
            "*专业级别（层次）", "*学制", "*学历",
            "*户口所在地-省", "*户口所在地-市", "*户口所在地-区", "*户口-地址", "*户口性质", "*邮政编码",
            "*是否建档立卡", "*资助申请类型",
            "*父亲姓名", "*父亲身份证号", "*母亲姓名", "*母亲身份证号",
            "*其他监护人姓名", "*其他监护人身份证号", "备注"
        };

        List<Object[]> data = new ArrayList<>();
        // 添加示例数据
        data.add(new Object[]{
            "1", "张三", "2024001", "身份证", "110101200601011234", "2006-01-01", "男", "汉族", "共青团员", "13800138000",
            "2024", "计算机系", "计算机应用", "001", "2024级1班", "李老师",
            "中专", "3年", "中专",
            "北京市", "北京市", "海淀区", "中关村大街1号", "非农业", "100000",
            "否", "无",
            "张父", "110101197001011234", "张母", "110101197201011234",
            "", "", ""
        });

        // 获取所有班级名称用于下拉选择
        List<com.school.management.entity.Class> classes = classMapper.selectList(
            new LambdaQueryWrapper<com.school.management.entity.Class>()
                .eq(com.school.management.entity.Class::getDeleted, 0)
                .orderByDesc(com.school.management.entity.Class::getGradeLevel)
                .orderByAsc(com.school.management.entity.Class::getClassName)
        );
        String[] classNames = classes.stream()
            .map(com.school.management.entity.Class::getClassName)
            .toArray(String[]::new);

        // 民族列表
        String[] ethnicities = {
            "汉族", "蒙古族", "回族", "藏族", "维吾尔族", "苗族", "彝族", "壮族", "布依族", "朝鲜族",
            "满族", "侗族", "瑶族", "白族", "土家族", "哈尼族", "哈萨克族", "傣族", "黎族", "傈僳族",
            "佤族", "畲族", "高山族", "拉祜族", "水族", "东乡族", "纳西族", "景颇族", "柯尔克孜族",
            "土族", "达斡尔族", "仫佬族", "羌族", "布朗族", "撒拉族", "毛南族", "仡佬族", "锡伯族",
            "阿昌族", "普米族", "塔吉克族", "怒族", "乌孜别克族", "俄罗斯族", "鄂温克族", "德昂族",
            "保安族", "裕固族", "京族", "塔塔尔族", "独龙族", "鄂伦春族", "赫哲族", "门巴族", "珞巴族", "基诺族"
        };

        // 政治面貌
        String[] politicalStatuses = {"群众", "共青团员", "中共党员", "中共预备党员", "民主党派", "无党派人士"};

        // 设置下拉选项
        java.util.Map<Integer, String[]> dropdownOptions = new java.util.HashMap<>();
        dropdownOptions.put(3, new String[]{"身份证", "护照", "港澳通行证", "台湾通行证", "其他"}); // 证件类型
        dropdownOptions.put(6, new String[]{"男", "女"}); // 性别
        dropdownOptions.put(7, ethnicities); // 民族
        dropdownOptions.put(8, politicalStatuses); // 政治面貌
        if (classNames.length > 0 && classNames.length <= 100) {
            dropdownOptions.put(14, classNames); // 班级
        }
        dropdownOptions.put(16, new String[]{"中专", "大专", "本科", "高职"}); // 专业级别
        dropdownOptions.put(17, new String[]{"1年", "2年", "3年", "4年", "5年"}); // 学制
        dropdownOptions.put(18, new String[]{"中专", "大专", "本科", "硕士", "博士"}); // 学历
        dropdownOptions.put(23, new String[]{"农业", "非农业"}); // 户口性质
        dropdownOptions.put(25, new String[]{"是", "否"}); // 是否建档立卡
        dropdownOptions.put(26, new String[]{"无", "低保户", "特困供养", "建档立卡", "残疾学生", "孤儿", "其他"}); // 资助类型

        return ExcelUtils.exportTemplateWithDropdown(headers, data, "学生信息导入模板", dropdownOptions);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 1: return "在读";
            case 2: return "休学";
            case 3: return "退学";
            case 4: return "毕业";
            default: return "未知";
        }
    }

    /**
     * 检查当前用户是否为班主任角色
     * @return 是否为班主任
     */
    private boolean isClassTeacher() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        // 查询用户角色
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.management.entity.UserRole> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(com.school.management.entity.UserRole::getUserId, currentUserId)
               .eq(com.school.management.entity.UserRole::getRoleId, CLASS_TEACHER_ROLE_ID);

        return userRoleMapper.selectCount(wrapper) > 0;
    }

    /**
     * 获取班主任管理的班级ID列表
     * @return 班级ID列表,如果不是班主任返回空列表
     */
    private List<Long> getManagedClassIds() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || !isClassTeacher()) {
            return new ArrayList<>();
        }

        // 查询该教师管理的所有班级
        List<com.school.management.dto.ClassResponse> classes = classMapper.selectByTeacherId(currentUserId);
        return classes.stream()
                     .map(com.school.management.dto.ClassResponse::getId)
                     .collect(Collectors.toList());
    }

    /**
     * 应用班主任权限过滤
     * 如果当前用户是班主任,则只返回其管理的班级的学生
     * @param request 查询请求
     */
    private void applyClassTeacherFilter(StudentQueryRequest request) {
        if (isClassTeacher()) {
            List<Long> managedClassIds = getManagedClassIds();

            if (managedClassIds.isEmpty()) {
                log.warn("班主任用户 {} 未管理任何班级,将无法查看学生数据", SecurityUtils.getCurrentUserId());
                // 设置一个不可能存在的班级ID,使查询结果为空
                request.setClassId(-1L);
            } else if (managedClassIds.size() == 1) {
                // 只管理一个班级,直接设置classId过滤
                log.info("班主任用户 {} 管理班级: {}", SecurityUtils.getCurrentUserId(), managedClassIds);
                request.setClassId(managedClassIds.get(0));
            } else {
                // 管理多个班级,设置classIds列表过滤
                log.info("班主任用户 {} 管理多个班级: {}", SecurityUtils.getCurrentUserId(), managedClassIds);
                request.setClassIds(managedClassIds);
                request.setClassId(null); // 清除单个classId
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getStudentsByClassId(Long classId) {
        log.info("根据班级ID获取学生列表: classId={}", classId);
        if (classId == null) {
            return new ArrayList<>();
        }
        return studentMapper.selectByClassId(classId);
    }

    /**
     * 自动生成学号
     * 格式：年份 + 6位序号，如 2024000001
     */
    private String generateStudentNo() {
        int year = java.time.LocalDate.now().getYear();
        String prefix = String.valueOf(year);

        // 查找当前年份最大的学号
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Student::getStudentNo, prefix);
        wrapper.orderByDesc(Student::getStudentNo);
        wrapper.last("LIMIT 1");

        Student lastStudent = studentMapper.selectOne(wrapper);

        int nextSeq = 1;
        if (lastStudent != null && lastStudent.getStudentNo() != null) {
            String lastNo = lastStudent.getStudentNo();
            if (lastNo.length() > 4) {
                try {
                    nextSeq = Integer.parseInt(lastNo.substring(4)) + 1;
                } catch (NumberFormatException e) {
                    // 解析失败，使用默认值1
                    log.warn("学号序号解析失败: {}", lastNo);
                }
            }
        }

        return prefix + String.format("%06d", nextSeq);
    }

    @Override
    public Object previewImportData(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        List<List<Object>> data = ExcelUtils.importExcel(file.getInputStream(), 1);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalRows", data.size());
        result.put("data", data.stream().limit(100).collect(Collectors.toList())); // 最多预览100行
        result.put("previewLimit", 100);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object confirmImport(java.util.List<java.util.Map<String, Object>> data) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new BusinessException("导入数据不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        java.util.List<java.util.Map<String, Object>> failedRecords = new java.util.ArrayList<>();

        for (java.util.Map<String, Object> row : data) {
            try {
                // 从Map中提取数据创建学生
                String studentNo = getStringValue(row, "学号");
                String realName = getStringValue(row, "姓名");
                String genderStr = getStringValue(row, "性别");
                String className = getStringValue(row, "班级");

                if (studentNo.isEmpty() || realName.isEmpty()) {
                    throw new BusinessException("学号和姓名不能为空");
                }

                // 查找班级ID
                com.school.management.entity.Class clazz = classMapper.selectOne(
                    new LambdaQueryWrapper<com.school.management.entity.Class>()
                        .eq(com.school.management.entity.Class::getClassName, className)
                        .eq(com.school.management.entity.Class::getDeleted, 0));
                if (clazz == null) {
                    throw new BusinessException("班级[" + className + "]不存在");
                }

                StudentCreateRequest request = new StudentCreateRequest();
                request.setStudentNo(studentNo);
                request.setRealName(realName);
                request.setGender("男".equals(genderStr) ? 1 : 2);
                request.setClassId(clazz.getId());
                request.setUsername(studentNo);
                request.setPassword("123456");
                request.setPhone(getStringValue(row, "联系方式"));
                request.setIdCard(getStringValue(row, "身份证号"));

                createStudent(request);
                successCount++;
            } catch (Exception e) {
                failCount++;
                row.put("errorMessage", e.getMessage());
                failedRecords.add(row);
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failedRecords", failedRecords);
        return result;
    }

    @Override
    public byte[] exportFailedData(java.util.List<java.util.Map<String, Object>> data) throws IOException {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        // 获取表头
        java.util.Set<String> headerSet = new java.util.LinkedHashSet<>();
        for (java.util.Map<String, Object> row : data) {
            headerSet.addAll(row.keySet());
        }
        String[] headers = headerSet.toArray(new String[0]);

        // 准备数据
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> row : data) {
            Object[] rowData = new Object[headers.length];
            for (int i = 0; i < headers.length; i++) {
                rowData[i] = row.get(headers[i]);
            }
            rows.add(rowData);
        }

        return ExcelUtils.exportExcel(headers, rows, "导入失败数据");
    }

    /**
     * 从Map中获取字符串值
     */
    private String getStringValue(java.util.Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString().trim() : "";
    }
}