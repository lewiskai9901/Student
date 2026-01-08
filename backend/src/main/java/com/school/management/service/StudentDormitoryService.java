package com.school.management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.*;

import java.util.List;

/**
 * 学生住宿服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface StudentDormitoryService {

    /**
     * 分页查询住宿记录
     */
    Page<StudentDormitoryResponse> getPage(StudentDormitoryQueryRequest request);

    /**
     * 查询学生当前住宿信息
     */
    StudentDormitoryResponse getCurrentByStudentId(Long studentId);

    /**
     * 查询宿舍当前入住学生列表
     */
    List<StudentDormitoryResponse> getCurrentByDormitoryId(Long dormitoryId);

    /**
     * 查询学生住宿历史
     */
    List<StudentDormitoryResponse> getHistoryByStudentId(Long studentId);

    /**
     * 学生入住
     */
    Long checkIn(StudentCheckInRequest request);

    /**
     * 学生退宿
     */
    void checkOut(StudentCheckOutRequest request);

    /**
     * 学生调换宿舍
     */
    void changeDormitory(StudentChangeDormitoryRequest request);

    /**
     * 批量入住
     */
    int batchCheckIn(List<StudentCheckInRequest> requests);

    /**
     * 批量退宿
     */
    int batchCheckOut(List<Long> studentIds, String reason);

    /**
     * 同步学生表的宿舍信息到中间表(用于数据迁移)
     */
    int syncFromStudentTable();
}
