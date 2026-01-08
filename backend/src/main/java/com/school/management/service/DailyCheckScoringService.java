package com.school.management.service;

import com.school.management.dto.DailyScoringRequest;
import com.school.management.dto.DailyScoringResponse;
import com.school.management.dto.ScoringDetailResponse;

import java.util.List;

/**
 * 日常检查打分服务接口
 *
 * @author system
 * @since 1.0.6
 */
public interface DailyCheckScoringService {

    /**
     * 保存打分数据(增量更新)
     *
     * @param request 打分请求
     */
    void saveScoring(DailyScoringRequest request);

    /**
     * 获取检查的打分数据
     *
     * @param checkId 检查ID
     * @return 打分响应
     */
    DailyScoringResponse getScoringByCheckId(Long checkId);

    /**
     * 获取某个检查某个班级的扣分明细
     *
     * @param checkId 检查ID
     * @param classId 班级ID
     * @return 扣分明细列表
     */
    List<ScoringDetailResponse> getDetailsByCheckIdAndClassId(Long checkId, Long classId);

    /**
     * 获取某个检查某个类别某个班级的扣分明细
     *
     * @param checkId    检查ID
     * @param categoryId 类别ID
     * @param classId    班级ID
     * @return 扣分明细列表
     */
    List<ScoringDetailResponse> getDetailsByCheckIdAndCategoryIdAndClassId(
            Long checkId, Long categoryId, Long classId);

    /**
     * 删除扣分明细
     *
     * @param detailId 明细ID
     */
    void deleteDetail(Long detailId);

    /**
     * 批量删除扣分明细
     *
     * @param detailIds 明细ID列表
     */
    void batchDeleteDetails(List<Long> detailIds);
}
