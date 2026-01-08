package com.school.management.mapper.record;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.record.CheckRecordDeductionNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 扣分明细Mapper（重构版）
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
@Repository("newCheckRecordDeductionMapper")
public interface CheckRecordDeductionMapper extends BaseMapper<CheckRecordDeductionNew> {

    /**
     * 根据检查记录ID查询所有扣分明细
     *
     * @param recordId 检查记录ID
     * @return 扣分明细列表
     */
    List<CheckRecordDeductionNew> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据班级统计ID查询扣分明细
     *
     * @param classStatId 班级统计ID
     * @return 扣分明细列表
     */
    List<CheckRecordDeductionNew> selectByClassStatId(@Param("classStatId") Long classStatId);

    /**
     * 根据类别统计ID查询扣分明细
     *
     * @param categoryStatId 类别统计ID
     * @return 扣分明细列表
     */
    List<CheckRecordDeductionNew> selectByCategoryStatId(@Param("categoryStatId") Long categoryStatId);

    /**
     * 根据班级ID查询扣分明细
     *
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @return 扣分明细列表
     */
    List<CheckRecordDeductionNew> selectByRecordAndClass(
            @Param("recordId") Long recordId,
            @Param("classId") Long classId
    );

    /**
     * 分页查询扣分明细
     *
     * @param page 分页参数
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @param categoryId 类别ID
     * @param appealStatus 申诉状态
     * @return 分页结果
     */
    IPage<CheckRecordDeductionNew> selectDeductionPage(
            Page<CheckRecordDeductionNew> page,
            @Param("recordId") Long recordId,
            @Param("classId") Long classId,
            @Param("categoryId") Long categoryId,
            @Param("appealStatus") Integer appealStatus
    );

    /**
     * 根据学生ID查询相关扣分明细
     *
     * @param recordId 检查记录ID
     * @param studentId 学生ID
     * @return 扣分明细列表
     */
    List<CheckRecordDeductionNew> selectByStudent(
            @Param("recordId") Long recordId,
            @Param("studentId") Long studentId
    );

    /**
     * 根据关联对象查询扣分明细（宿舍/教室）
     *
     * @param recordId 检查记录ID
     * @param linkType 关联类型
     * @param linkId 关联对象ID
     * @return 扣分明细列表
     */
    List<CheckRecordDeductionNew> selectByLink(
            @Param("recordId") Long recordId,
            @Param("linkType") Integer linkType,
            @Param("linkId") Long linkId
    );

    /**
     * 查询待申诉的扣分明细
     *
     * @param recordId 检查记录ID
     * @return 扣分明细列表
     */
    List<CheckRecordDeductionNew> selectPendingAppeals(@Param("recordId") Long recordId);

    /**
     * 更新申诉状态
     *
     * @param deductionId 扣分明细ID
     * @param appealStatus 申诉状态
     * @param appealId 申诉ID
     * @param appealResultRemark 申诉结果说明
     * @return 更新行数
     */
    int updateAppealStatus(
            @Param("deductionId") Long deductionId,
            @Param("appealStatus") Integer appealStatus,
            @Param("appealId") Long appealId,
            @Param("appealResultRemark") String appealResultRemark
    );

    /**
     * 修订扣分
     *
     * @param deduction 扣分明细（包含修订信息）
     * @return 更新行数
     */
    int reviseDeduction(CheckRecordDeductionNew deduction);

    /**
     * 批量插入扣分明细
     *
     * @param list 扣分明细列表
     * @return 插入行数
     */
    int batchInsert(@Param("list") List<CheckRecordDeductionNew> list);

    /**
     * 删除指定检查记录的所有扣分明细
     *
     * @param recordId 检查记录ID
     * @return 删除行数
     */
    int deleteByRecordId(@Param("recordId") Long recordId);

    /**
     * 统计指定班级的扣分项数量
     *
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @return 扣分项数量
     */
    int countByClass(
            @Param("recordId") Long recordId,
            @Param("classId") Long classId
    );

    /**
     * 计算班级在某次检查中的总扣分
     *
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @return 总扣分
     */
    java.math.BigDecimal sumActualScoreByRecordAndClass(
            @Param("recordId") Long recordId,
            @Param("classId") Long classId
    );

    /**
     * 更新扣分明细的实际扣分（申诉通过后调整）
     *
     * @param deductionId 扣分明细ID
     * @param actualScore 调整后的扣分
     * @return 更新行数
     */
    int updateActualScore(
            @Param("deductionId") Long deductionId,
            @Param("actualScore") java.math.BigDecimal actualScore
    );
}
