package com.school.management.service.rating;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.rating.RatingResultQueryDTO;
import com.school.management.dto.rating.RatingResultVO;
import com.school.management.entity.rating.RatingResult;

import java.util.List;

/**
 * 评级结果服务
 *
 * @author System
 * @since 4.4.0
 */
public interface RatingResultService extends IService<RatingResult> {

    /**
     * 审核评级结果
     *
     * @param resultId 结果ID
     * @param approved 是否通过
     * @param userId 审核人ID
     */
    void approveResult(Long resultId, boolean approved, Long userId);

    /**
     * 批量审核评级结果
     *
     * @param resultIds 结果ID列表
     * @param approved 是否通过
     * @param userId 审核人ID
     */
    void batchApproveResults(List<Long> resultIds, boolean approved, Long userId);

    /**
     * 发布评级结果
     *
     * @param resultId 结果ID
     * @param userId 发布人ID
     */
    void publishResult(Long resultId, Long userId);

    /**
     * 批量发布评级结果
     *
     * @param resultIds 结果ID列表
     * @param userId 发布人ID
     */
    void batchPublishResults(List<Long> resultIds, Long userId);

    /**
     * 撤销发布
     *
     * @param resultId 结果ID
     * @param userId 操作人ID
     */
    void revokeResult(Long resultId, Long userId);

    /**
     * 获取评级结果详情
     *
     * @param resultId 结果ID
     * @return 结果VO
     */
    RatingResultVO getResultDetail(Long resultId);

    /**
     * 分页查询评级结果
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<RatingResultVO> getResultPage(RatingResultQueryDTO query);

    /**
     * 获取班级的评级历史
     *
     * @param classId 班级ID
     * @param ratingConfigId 评级配置ID（可选）
     * @return 评级历史列表
     */
    List<RatingResultVO> getClassRatingHistory(Long classId, Long ratingConfigId);
}
