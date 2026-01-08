package com.school.management.service.record;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.record.*;

import java.util.List;

/**
 * 检查记录申诉服务接口（重构版）
 *
 * @author system
 * @since 2.0.0
 */
public interface CheckRecordAppealService {

    /**
     * 分页查询申诉列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<CheckRecordAppealDTO> queryPage(CheckRecordAppealQueryDTO query);

    /**
     * 获取申诉详情
     *
     * @param appealId 申诉ID
     * @return 申诉详情
     */
    CheckRecordAppealDTO getById(Long appealId);

    /**
     * 创建申诉
     *
     * @param createDTO 创建参数
     * @param appellantId 申诉人ID
     * @param appellantName 申诉人姓名
     * @param appellantRole 申诉人角色
     * @return 申诉详情
     */
    CheckRecordAppealDTO create(CheckRecordAppealCreateDTO createDTO, Long appellantId, String appellantName, String appellantRole);

    /**
     * 处理申诉
     *
     * @param handleDTO 处理参数
     * @param handlerId 处理人ID
     * @param handlerName 处理人姓名
     */
    void handle(CheckRecordAppealHandleDTO handleDTO, Long handlerId, String handlerName);

    /**
     * 获取待处理申诉列表
     *
     * @param recordId 记录ID
     * @return 待处理申诉列表
     */
    List<CheckRecordAppealDTO> getPendingAppeals(Long recordId);

    /**
     * 获取班级申诉列表
     *
     * @param recordId 记录ID
     * @param classId 班级ID
     * @return 申诉列表
     */
    List<CheckRecordAppealDTO> getByClass(Long recordId, Long classId);

    /**
     * 检查是否可以申诉
     *
     * @param deductionId 扣分明细ID
     * @return 是否可以申诉
     */
    boolean canAppeal(Long deductionId);
}
