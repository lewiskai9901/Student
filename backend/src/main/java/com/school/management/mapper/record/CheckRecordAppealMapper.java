package com.school.management.mapper.record;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.record.CheckRecordAppealNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 检查记录申诉Mapper（重构版）
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
@Repository("newCheckRecordAppealMapper")
public interface CheckRecordAppealMapper extends BaseMapper<CheckRecordAppealNew> {

    /**
     * 根据检查记录ID查询所有申诉
     *
     * @param recordId 检查记录ID
     * @return 申诉列表
     */
    List<CheckRecordAppealNew> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据扣分明细ID查询申诉
     *
     * @param deductionId 扣分明细ID
     * @return 申诉记录
     */
    CheckRecordAppealNew selectByDeductionId(@Param("deductionId") Long deductionId);

    /**
     * 根据班级ID查询申诉列表
     *
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @return 申诉列表
     */
    List<CheckRecordAppealNew> selectByClass(
            @Param("recordId") Long recordId,
            @Param("classId") Long classId
    );

    /**
     * 分页查询申诉列表
     *
     * @param page 分页参数
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @param status 状态
     * @param appellantRole 申诉人角色
     * @return 分页结果
     */
    IPage<CheckRecordAppealNew> selectAppealPage(
            Page<CheckRecordAppealNew> page,
            @Param("recordId") Long recordId,
            @Param("classId") Long classId,
            @Param("status") Integer status,
            @Param("appellantRole") String appellantRole
    );

    /**
     * 查询待处理的申诉
     *
     * @param recordId 检查记录ID
     * @return 申诉列表
     */
    List<CheckRecordAppealNew> selectPendingAppeals(@Param("recordId") Long recordId);

    /**
     * 查询申诉人的申诉历史
     *
     * @param appellantId 申诉人ID
     * @param limit 限制数量
     * @return 申诉列表
     */
    List<CheckRecordAppealNew> selectByAppellant(
            @Param("appellantId") Long appellantId,
            @Param("limit") Integer limit
    );

    /**
     * 查询处理人的处理历史
     *
     * @param handlerId 处理人ID
     * @param limit 限制数量
     * @return 申诉列表
     */
    List<CheckRecordAppealNew> selectByHandler(
            @Param("handlerId") Long handlerId,
            @Param("limit") Integer limit
    );

    /**
     * 更新申诉状态为处理中
     *
     * @param appealId 申诉ID
     * @param handlerId 处理人ID
     * @param handlerName 处理人姓名
     * @return 更新行数
     */
    int startProcessing(
            @Param("appealId") Long appealId,
            @Param("handlerId") Long handlerId,
            @Param("handlerName") String handlerName
    );

    /**
     * 通过申诉
     *
     * @param appeal 申诉记录（包含处理结果）
     * @return 更新行数
     */
    int approveAppeal(CheckRecordAppealNew appeal);

    /**
     * 驳回申诉
     *
     * @param appeal 申诉记录（包含处理结果）
     * @return 更新行数
     */
    int rejectAppeal(CheckRecordAppealNew appeal);

    /**
     * 统计检查记录的申诉数量
     *
     * @param recordId 检查记录ID
     * @param status 状态（可选）
     * @return 申诉数量
     */
    int countByRecord(
            @Param("recordId") Long recordId,
            @Param("status") Integer status
    );

    /**
     * 检查是否已存在申诉
     *
     * @param deductionId 扣分明细ID
     * @return 是否存在
     */
    boolean existsByDeductionId(@Param("deductionId") Long deductionId);
}
