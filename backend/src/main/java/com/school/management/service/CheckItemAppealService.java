package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.CheckItemAppealDTO;
import com.school.management.dto.request.AppealCreateRequest;
import com.school.management.dto.request.AppealReviewRequest;
import com.school.management.dto.vo.AppealListVO;
import com.school.management.dto.vo.AppealStatisticsVO;
import com.school.management.dto.vo.RankingChangeVO;
import com.school.management.entity.CheckItemAppeal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 申诉管理服务接口
 * 核心业务:申诉提交、审核、公示、生效
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
public interface CheckItemAppealService extends IService<CheckItemAppeal> {

    /**
     * 提交申诉
     *
     * @param request 申诉创建请求
     * @param appellantId 申诉人ID
     * @return 申诉详情DTO
     */
    CheckItemAppealDTO createAppeal(AppealCreateRequest request, Long appellantId);

    /**
     * 审核申诉
     *
     * @param request 审核请求
     * @param approverId 审批人ID
     * @return 申诉详情DTO
     */
    CheckItemAppealDTO reviewAppeal(AppealReviewRequest request, Long approverId);

    /**
     * 撤销申诉
     *
     * @param appealId 申诉ID
     * @param reason 撤销原因
     * @param userId 操作人ID
     * @return 是否成功
     */
    boolean withdrawAppeal(Long appealId, String reason, Long userId);

    /**
     * 处理公示期结束
     * 定时任务调用,将公示期满的申诉自动生效
     *
     * @return 处理数量
     */
    int processPublicityEnd();

    /**
     * 申诉生效
     * 更新扣分、重算排名、发送通知
     *
     * @param appealId 申诉ID
     * @return 排名变化VO
     */
    RankingChangeVO processEffective(Long appealId);

    /**
     * 申诉生效后重新计算所有相关数据
     *
     * @param appeal 申诉实体
     * @return 排名变化VO
     */
    RankingChangeVO recalculateAfterAppeal(CheckItemAppeal appeal);

    /**
     * 分页查询申诉列表
     *
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param gradeId 年级ID
     * @param classId 班级ID
     * @param appellantId 申诉人ID
     * @param status 状态
     * @param appealType 申诉类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param keyword 关键词
     * @return 申诉列表分页
     */
    IPage<AppealListVO> listAppeals(Integer pageNum, Integer pageSize,
                                     Long gradeId, Long classId, Long appellantId,
                                     Integer status, Integer appealType,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     String keyword);

    /**
     * 查询待审核的申诉列表(指定审批人)
     *
     * @param approverId 审批人ID
     * @return 申诉列表
     */
    List<AppealListVO> listPendingAppeals(Long approverId);

    /**
     * 查询公示中的申诉列表
     *
     * @return 申诉列表
     */
    List<AppealListVO> listPublicityAppeals();

    /**
     * 查询班级的申诉历史
     *
     * @param classId 班级ID
     * @param limit 限制数量
     * @return 申诉列表
     */
    List<AppealListVO> listAppealHistory(Long classId, Integer limit);

    /**
     * 根据ID查询申诉详情
     *
     * @param appealId 申诉ID
     * @return 申诉详情DTO
     */
    CheckItemAppealDTO getAppealDetail(Long appealId);

    /**
     * 根据申诉编号查询申诉
     *
     * @param appealCode 申诉编号
     * @return 申诉详情DTO
     */
    CheckItemAppealDTO getAppealByCode(String appealCode);

    /**
     * 检查扣分明细是否可以申诉
     *
     * @param itemId 扣分明细ID
     * @return 可申诉返回null,不可申诉返回原因
     */
    String checkCanAppeal(Long itemId);

    /**
     * 检查申诉是否可以撤销
     *
     * @param appealId 申诉ID
     * @param userId 用户ID
     * @return 可撤销返回null,不可撤销返回原因
     */
    String checkCanWithdraw(Long appealId, Long userId);

    /**
     * 检查申诉是否可以审核
     *
     * @param appealId 申诉ID
     * @param approverId 审批人ID
     * @return 可审核返回null,不可审核返回原因
     */
    String checkCanReview(Long appealId, Long approverId);

    /**
     * 获取申诉统计数据
     *
     * @param scope 统计范围(OVERALL/GRADE/CLASS/USER)
     * @param scopeId 范围ID
     * @param period 统计周期(DAY/WEEK/MONTH/SEMESTER/YEAR)
     * @return 统计VO
     */
    AppealStatisticsVO getAppealStatistics(String scope, Long scopeId, String period);

    /**
     * 生成申诉编号
     *
     * @return 申诉编号
     */
    String generateAppealCode();

    /**
     * 实体转DTO
     *
     * @param appeal 申诉实体
     * @return 申诉DTO
     */
    CheckItemAppealDTO convertToDTO(CheckItemAppeal appeal);

    /**
     * 实体转ListVO
     *
     * @param appeal 申诉实体
     * @return 申诉ListVO
     */
    AppealListVO convertToListVO(CheckItemAppeal appeal);

    /**
     * 实体列表转DTO列表
     *
     * @param appeals 申诉实体列表
     * @return 申诉DTO列表
     */
    List<CheckItemAppealDTO> convertToDTOList(List<CheckItemAppeal> appeals);

    /**
     * 实体列表转ListVO列表
     *
     * @param appeals 申诉实体列表
     * @return 申诉ListVO列表
     */
    List<AppealListVO> convertToListVOList(List<CheckItemAppeal> appeals);
}
