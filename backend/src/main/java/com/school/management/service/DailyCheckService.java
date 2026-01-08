package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.DailyCheckCreateRequest;
import com.school.management.dto.DailyCheckResponse;
import com.school.management.dto.DailyScoringInitResponse;
import com.school.management.dto.DailyScoringRequest;

import java.time.LocalDate;

/**
 * 日常检查服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface DailyCheckService {

    /**
     * 创建日常检查
     *
     * @param request 创建请求
     * @return 检查ID
     */
    Long createDailyCheck(DailyCheckCreateRequest request);

    /**
     * 更新日常检查
     *
     * @param id 检查ID
     * @param request 更新请求
     */
    void updateDailyCheck(Long id, DailyCheckCreateRequest request);

    /**
     * 删除日常检查
     *
     * @param id 检查ID
     */
    void deleteDailyCheck(Long id);

    /**
     * 获取检查详情
     *
     * @param id 检查ID
     * @return 检查详情
     */
    DailyCheckResponse getDailyCheckById(Long id);

    /**
     * 分页查询检查
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param checkDate 检查日期
     * @param status 状态
     * @param checkName 检查名称
     * @param planId 检查计划ID
     * @return 分页结果
     */
    IPage<DailyCheckResponse> getDailyCheckPage(Integer pageNum, Integer pageSize,
                                                  LocalDate checkDate, Integer status, String checkName, Long planId);

    /**
     * 更新检查状态
     *
     * @param id 检查ID
     * @param status 新状态
     */
    void updateCheckStatus(Long id, Integer status);

    /**
     * 获取打分初始化数据
     *
     * @param checkId 检查ID
     * @return 打分初始化数据
     */
    DailyScoringInitResponse getScoringInitData(Long checkId);

    /**
     * 保存打分数据
     *
     * @param checkId 检查ID
     * @param request 打分请求
     */
    void saveScoring(Long checkId, DailyScoringRequest request);
}
