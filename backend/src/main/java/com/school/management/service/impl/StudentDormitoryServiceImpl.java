package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.*;
import com.school.management.entity.Dormitory;
import com.school.management.entity.Student;
import com.school.management.entity.StudentDormitory;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.DormitoryMapper;
import com.school.management.mapper.StudentDormitoryMapper;
import com.school.management.mapper.StudentMapper;
import com.school.management.service.StudentDormitoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生住宿服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentDormitoryServiceImpl extends ServiceImpl<StudentDormitoryMapper, StudentDormitory> implements StudentDormitoryService {

    private final StudentDormitoryMapper studentDormitoryMapper;
    private final StudentMapper studentMapper;
    private final DormitoryMapper dormitoryMapper;

    // 状态常量
    private static final int STATUS_LIVING = 1;      // 在住
    private static final int STATUS_CHECKED_OUT = 2; // 已退宿
    private static final int STATUS_CHANGING = 3;    // 调换中

    @Override
    public Page<StudentDormitoryResponse> getPage(StudentDormitoryQueryRequest request) {
        Page<StudentDormitory> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<StudentDormitory> result = studentDormitoryMapper.selectPageWithDetails(page, request);

        Page<StudentDormitoryResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        return responsePage;
    }

    @Override
    public StudentDormitoryResponse getCurrentByStudentId(Long studentId) {
        StudentDormitory record = studentDormitoryMapper.selectCurrentByStudentId(studentId);
        return record != null ? convertToResponse(record) : null;
    }

    @Override
    public List<StudentDormitoryResponse> getCurrentByDormitoryId(Long dormitoryId) {
        List<StudentDormitory> records = studentDormitoryMapper.selectCurrentByDormitoryId(dormitoryId);
        return records.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StudentDormitoryResponse> getHistoryByStudentId(Long studentId) {
        List<StudentDormitory> records = studentDormitoryMapper.selectHistoryByStudentId(studentId);
        return records.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long checkIn(StudentCheckInRequest request) {
        log.info("学生入住: studentId={}, dormitoryId={}, bedNumber={}",
                request.getStudentId(), request.getDormitoryId(), request.getBedNumber());

        // 1. 验证学生是否存在
        Student student = studentMapper.selectById(request.getStudentId());
        if (student == null || student.getDeleted() == 1) {
            throw new BusinessException("学生不存在");
        }

        // 2. 验证宿舍是否存在
        Dormitory dormitory = dormitoryMapper.selectById(request.getDormitoryId());
        if (dormitory == null || dormitory.getDeleted() == 1) {
            throw new BusinessException("宿舍不存在");
        }

        // 3. 检查学生是否已有在住记录
        StudentDormitory existing = studentDormitoryMapper.selectCurrentByStudentId(request.getStudentId());
        if (existing != null) {
            throw new BusinessException("该学生已有在住宿舍，请先办理退宿或调换");
        }

        // 4. 检查宿舍是否已满
        Integer currentOccupancy = studentDormitoryMapper.countCurrentOccupancy(request.getDormitoryId());
        if (currentOccupancy >= dormitory.getBedCount()) {
            throw new BusinessException("该宿舍已满，无法入住");
        }

        // 5. 检查床位是否已被占用
        if (request.getBedNumber() != null && !request.getBedNumber().isEmpty()) {
            Integer bedOccupied = studentDormitoryMapper.checkBedOccupied(
                    request.getDormitoryId(), request.getBedNumber(), null);
            if (bedOccupied > 0) {
                throw new BusinessException("该床位已被占用");
            }
        }

        // 6. 创建入住记录
        StudentDormitory record = new StudentDormitory();
        record.setStudentId(request.getStudentId());
        record.setDormitoryId(request.getDormitoryId());
        record.setBedNumber(request.getBedNumber());
        record.setCheckInDate(request.getCheckInDate() != null ? request.getCheckInDate() : LocalDate.now());
        record.setStatus(STATUS_LIVING);
        record.setRemark(request.getRemark());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.setDeleted(0);

        studentDormitoryMapper.insert(record);

        // 7. 同步更新学生表(保持兼容性)
        student.setDormitoryId(request.getDormitoryId());
        student.setBedNumber(request.getBedNumber());
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.updateById(student);

        // 8. 更新宿舍已占用床位数
        dormitory.setOccupiedBeds(currentOccupancy + 1);
        dormitory.setUpdatedAt(LocalDateTime.now());
        dormitoryMapper.updateById(dormitory);

        log.info("学生入住成功: recordId={}", record.getId());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkOut(StudentCheckOutRequest request) {
        log.info("学生退宿: studentId={}", request.getStudentId());

        // 1. 查询当前住宿记录
        StudentDormitory record = studentDormitoryMapper.selectCurrentByStudentId(request.getStudentId());
        if (record == null) {
            throw new BusinessException("该学生没有在住记录");
        }

        // 2. 更新住宿记录
        record.setCheckOutDate(request.getCheckOutDate() != null ? request.getCheckOutDate() : LocalDate.now());
        record.setStatus(STATUS_CHECKED_OUT);
        record.setChangeReason(request.getReason());
        record.setUpdatedAt(LocalDateTime.now());
        studentDormitoryMapper.updateById(record);

        // 3. 清空学生表的宿舍信息
        Student student = studentMapper.selectById(request.getStudentId());
        if (student != null) {
            student.setDormitoryId(null);
            student.setBedNumber(null);
            student.setUpdatedAt(LocalDateTime.now());
            studentMapper.updateById(student);
        }

        // 4. 更新宿舍已占用床位数
        Dormitory dormitory = dormitoryMapper.selectById(record.getDormitoryId());
        if (dormitory != null) {
            Integer currentOccupancy = studentDormitoryMapper.countCurrentOccupancy(record.getDormitoryId());
            dormitory.setOccupiedBeds(currentOccupancy);
            dormitory.setUpdatedAt(LocalDateTime.now());
            dormitoryMapper.updateById(dormitory);
        }

        log.info("学生退宿成功: studentId={}", request.getStudentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeDormitory(StudentChangeDormitoryRequest request) {
        log.info("学生调换宿舍: studentId={}, newDormitoryId={}",
                request.getStudentId(), request.getNewDormitoryId());

        // 1. 查询当前住宿记录
        StudentDormitory oldRecord = studentDormitoryMapper.selectCurrentByStudentId(request.getStudentId());
        if (oldRecord == null) {
            throw new BusinessException("该学生没有在住记录，请直接办理入住");
        }

        // 2. 验证新宿舍
        Dormitory newDormitory = dormitoryMapper.selectById(request.getNewDormitoryId());
        if (newDormitory == null || newDormitory.getDeleted() == 1) {
            throw new BusinessException("新宿舍不存在");
        }

        // 3. 检查新宿舍是否已满
        Integer newOccupancy = studentDormitoryMapper.countCurrentOccupancy(request.getNewDormitoryId());
        if (newOccupancy >= newDormitory.getBedCount()) {
            throw new BusinessException("新宿舍已满，无法调换");
        }

        // 4. 检查新床位是否已被占用
        if (request.getNewBedNumber() != null && !request.getNewBedNumber().isEmpty()) {
            Integer bedOccupied = studentDormitoryMapper.checkBedOccupied(
                    request.getNewDormitoryId(), request.getNewBedNumber(), null);
            if (bedOccupied > 0) {
                throw new BusinessException("新床位已被占用");
            }
        }

        LocalDate changeDate = request.getChangeDate() != null ? request.getChangeDate() : LocalDate.now();

        // 5. 更新旧记录为已退宿
        oldRecord.setCheckOutDate(changeDate);
        oldRecord.setStatus(STATUS_CHECKED_OUT);
        oldRecord.setChangeReason("调换宿舍: " + (request.getReason() != null ? request.getReason() : ""));
        oldRecord.setUpdatedAt(LocalDateTime.now());
        studentDormitoryMapper.updateById(oldRecord);

        // 6. 创建新入住记录
        StudentDormitory newRecord = new StudentDormitory();
        newRecord.setStudentId(request.getStudentId());
        newRecord.setDormitoryId(request.getNewDormitoryId());
        newRecord.setBedNumber(request.getNewBedNumber());
        newRecord.setCheckInDate(changeDate);
        newRecord.setStatus(STATUS_LIVING);
        newRecord.setChangeReason("从" + oldRecord.getDormitoryNo() + "调换");
        newRecord.setCreatedAt(LocalDateTime.now());
        newRecord.setUpdatedAt(LocalDateTime.now());
        newRecord.setDeleted(0);
        studentDormitoryMapper.insert(newRecord);

        // 7. 同步更新学生表
        Student student = studentMapper.selectById(request.getStudentId());
        if (student != null) {
            student.setDormitoryId(request.getNewDormitoryId());
            student.setBedNumber(request.getNewBedNumber());
            student.setUpdatedAt(LocalDateTime.now());
            studentMapper.updateById(student);
        }

        // 8. 更新两个宿舍的已占用床位数
        Dormitory oldDormitory = dormitoryMapper.selectById(oldRecord.getDormitoryId());
        if (oldDormitory != null) {
            Integer oldOccupancy = studentDormitoryMapper.countCurrentOccupancy(oldRecord.getDormitoryId());
            oldDormitory.setOccupiedBeds(oldOccupancy);
            oldDormitory.setUpdatedAt(LocalDateTime.now());
            dormitoryMapper.updateById(oldDormitory);
        }

        newDormitory.setOccupiedBeds(newOccupancy + 1);
        newDormitory.setUpdatedAt(LocalDateTime.now());
        dormitoryMapper.updateById(newDormitory);

        log.info("学生调换宿舍成功: studentId={}", request.getStudentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCheckIn(List<StudentCheckInRequest> requests) {
        int successCount = 0;
        for (StudentCheckInRequest request : requests) {
            try {
                checkIn(request);
                successCount++;
            } catch (Exception e) {
                log.warn("批量入住失败: studentId={}, error={}", request.getStudentId(), e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCheckOut(List<Long> studentIds, String reason) {
        int successCount = 0;
        for (Long studentId : studentIds) {
            try {
                StudentCheckOutRequest request = new StudentCheckOutRequest();
                request.setStudentId(studentId);
                request.setReason(reason);
                checkOut(request);
                successCount++;
            } catch (Exception e) {
                log.warn("批量退宿失败: studentId={}, error={}", studentId, e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromStudentTable() {
        log.info("开始同步学生宿舍数据到中间表...");

        // 查询所有有宿舍信息的学生
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Student::getDormitoryId)
               .eq(Student::getDeleted, 0);
        List<Student> students = studentMapper.selectList(wrapper);

        int syncCount = 0;
        for (Student student : students) {
            // 检查是否已有在住记录
            StudentDormitory existing = studentDormitoryMapper.selectCurrentByStudentId(student.getId());
            if (existing != null) {
                continue; // 已有记录，跳过
            }

            // 创建入住记录
            StudentDormitory record = new StudentDormitory();
            record.setStudentId(student.getId());
            record.setDormitoryId(student.getDormitoryId());
            record.setBedNumber(student.getBedNumber());
            record.setCheckInDate(student.getAdmissionDate() != null ? student.getAdmissionDate() : LocalDate.now());
            record.setStatus(STATUS_LIVING);
            record.setRemark("数据迁移自动生成");
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.setDeleted(0);

            studentDormitoryMapper.insert(record);
            syncCount++;
        }

        log.info("同步完成，共同步{}条记录", syncCount);
        return syncCount;
    }

    /**
     * 转换为响应DTO
     */
    private StudentDormitoryResponse convertToResponse(StudentDormitory entity) {
        StudentDormitoryResponse response = new StudentDormitoryResponse();
        BeanUtils.copyProperties(entity, response);

        // 设置状态名称
        response.setStatusName(getStatusName(entity.getStatus()));

        return response;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case STATUS_LIVING: return "在住";
            case STATUS_CHECKED_OUT: return "已退宿";
            case STATUS_CHANGING: return "调换中";
            default: return "未知";
        }
    }
}
