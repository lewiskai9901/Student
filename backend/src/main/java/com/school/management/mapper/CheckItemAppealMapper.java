package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.CheckItemAppeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 申诉Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface CheckItemAppealMapper extends BaseMapper<CheckItemAppeal> {

    /**
     * 分页查询申诉列表(带关联信息)
     *
     * @param page 分页对象
     * @param gradeId 年级ID
     * @param classId 班级ID
     * @param appellantId 申诉人ID
     * @param status 状态
     * @param appealType 申诉类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param keyword 关键词
     * @return 申诉列表
     */
    IPage<CheckItemAppeal> selectAppealPageWithDetails(
            Page<CheckItemAppeal> page,
            @Param("gradeId") Long gradeId,
            @Param("classId") Long classId,
            @Param("appellantId") Long appellantId,
            @Param("status") Integer status,
            @Param("appealType") Integer appealType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("keyword") String keyword
    );

    /**
     * 根据ID查询申诉详情(带所有关联信息)
     *
     * @param id 申诉ID
     * @return 申诉详情
     */
    CheckItemAppeal selectAppealWithFullDetails(@Param("id") Long id);

    /**
     * 根据申诉编号查询申诉
     *
     * @param appealCode 申诉编号
     * @return 申诉
     */
    CheckItemAppeal selectByAppealCode(@Param("appealCode") String appealCode);

    /**
     * 查询待审核的申诉列表(指定审批人)
     *
     * @param approverId 审批人ID
     * @return 申诉列表
     */
    List<CheckItemAppeal> selectPendingAppealsByApprover(@Param("approverId") Long approverId);

    /**
     * 查询公示中的申诉列表
     *
     * @return 申诉列表
     */
    List<CheckItemAppeal> selectPublicityAppeals();

    /**
     * 查询公示期已结束的申诉列表
     *
     * @param currentTime 当前时间
     * @return 申诉列表
     */
    List<CheckItemAppeal> selectPublicityEndedAppeals(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 查询超时未审批的申诉列表
     *
     * @param timeoutHours 超时小时数
     * @return 申诉列表
     */
    List<CheckItemAppeal> selectTimeoutAppeals(@Param("timeoutHours") Integer timeoutHours);

    /**
     * 查询班级的申诉历史
     *
     * @param classId 班级ID
     * @param limit 限制数量
     * @return 申诉列表
     */
    List<CheckItemAppeal> selectAppealHistoryByClass(
            @Param("classId") Long classId,
            @Param("limit") Integer limit
    );

    /**
     * 查询检查记录的申诉列表
     *
     * @param recordId 检查记录ID
     * @return 申诉列表
     */
    List<CheckItemAppeal> selectAppealsByRecordId(@Param("recordId") Long recordId);

    /**
     * 查询扣分明细的申诉记录
     *
     * @param itemId 扣分明细ID
     * @return 申诉
     */
    CheckItemAppeal selectByItemId(@Param("itemId") Long itemId);

    /**
     * 统计申诉数据(按状态)
     *
     * @param gradeId 年级ID
     * @param classId 班级ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    Map<String, Object> statisticsAppealsByStatus(
            @Param("gradeId") Long gradeId,
            @Param("classId") Long classId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计申诉数据(按类型)
     *
     * @param gradeId 年级ID
     * @param classId 班级ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表
     */
    List<Map<String, Object>> statisticsAppealsByType(
            @Param("gradeId") Long gradeId,
            @Param("classId") Long classId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计审批人的审批数据
     *
     * @param approverId 审批人ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    Map<String, Object> statisticsApprovalsByApprover(
            @Param("approverId") Long approverId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 批量更新申诉状态
     *
     * @param appealIds 申诉ID列表
     * @param status 状态
     * @return 更新行数
     */
    int batchUpdateStatus(
            @Param("appealIds") List<Long> appealIds,
            @Param("status") Integer status
    );

    /**
     * 更新申诉审批步骤
     *
     * @param appealId 申诉ID
     * @param currentStep 当前步骤
     * @param currentApproverId 当前审批人ID
     * @return 更新行数
     */
    int updateApprovalStep(
            @Param("appealId") Long appealId,
            @Param("currentStep") Integer currentStep,
            @Param("currentApproverId") Long currentApproverId
    );

    /**
     * 检查扣分明细是否已有申诉
     *
     * @param itemId 扣分明细ID
     * @param excludeStatuses 排除的状态列表
     * @return 申诉数量
     */
    int checkItemHasAppeal(
            @Param("itemId") Long itemId,
            @Param("excludeStatuses") List<Integer> excludeStatuses
    );

    /**
     * 根据范围查询申诉数据(用于统计)
     *
     * @param scope 范围类型(class/grade/school)
     * @param scopeId 范围ID
     * @param period 时间段
     * @return 申诉列表
     */
    List<CheckItemAppeal> selectAppealsByScope(
            @Param("scope") String scope,
            @Param("scopeId") Long scopeId,
            @Param("period") String period
    );

    /**
     * 统计年级申诉数据(按时间段)
     *
     * @param gradeId 年级ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果: totalCount, approvedCount, totalScoreAdjustment
     */
    Map<String, Object> statisticsAppealsByGradeAndTime(
            @Param("gradeId") Long gradeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
