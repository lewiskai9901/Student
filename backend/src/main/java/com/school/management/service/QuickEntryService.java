package com.school.management.service;

import com.school.management.dto.quickentry.*;

import java.util.List;

/**
 * 快捷录入服务接口
 *
 * @author system
 * @since 1.0.7
 */
public interface QuickEntryService {

    /**
     * 获取可用的按人次扣分项列表
     *
     * @param checkId 日常检查ID
     * @return 扣分项列表
     */
    List<QuickEntryDeductionItemDTO> getAvailableDeductionItems(Long checkId);

    /**
     * 搜索学生
     *
     * @param checkId 日常检查ID
     * @param keyword 搜索关键词 (姓名或学号)
     * @param limit   返回数量限制
     * @return 学生列表
     */
    List<QuickEntryStudentDTO> searchStudents(Long checkId, String keyword, Integer limit);

    /**
     * 检查是否存在重复记录
     *
     * @param checkId 日常检查ID
     * @param request 检查重复请求
     * @return 重复检查结果
     */
    QuickEntryCheckDuplicateResponse checkDuplicate(Long checkId, QuickEntryCheckDuplicateRequest request);

    /**
     * 提交快捷录入记录
     *
     * @param checkId   日常检查ID
     * @param request   提交请求
     * @param operatorId 操作人ID
     * @return 创建的记录
     */
    QuickEntryRecordDTO submitEntry(Long checkId, QuickEntrySubmitRequest request, Long operatorId);

    /**
     * 撤销录入记录
     *
     * @param checkId    日常检查ID
     * @param recordId   记录ID
     * @param operatorId 操作人ID
     */
    void revokeEntry(Long checkId, Long recordId, Long operatorId);

    /**
     * 获取本次检查的快捷录入记录列表
     *
     * @param checkId    日常检查ID
     * @param operatorId 操作人ID (可选,为空则返回所有记录)
     * @return 记录列表
     */
    List<QuickEntryRecordDTO> getMyEntryRecords(Long checkId, Long operatorId);
}
