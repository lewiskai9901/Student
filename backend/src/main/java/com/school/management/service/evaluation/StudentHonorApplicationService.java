package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.StudentHonorApplication;

import java.util.List;
import java.util.Map;

/**
 * 学生荣誉申报服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface StudentHonorApplicationService extends IService<StudentHonorApplication> {

    /**
     * 分页查询荣誉申报
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<Map<String, Object>> pageApplications(Page<?> page, Map<String, Object> query);

    /**
     * 学生提交荣誉申报
     *
     * @param application 申报信息
     * @return 申报ID
     */
    Long submitApplication(StudentHonorApplication application);

    /**
     * 获取申报详情
     *
     * @param id 申报ID
     * @return 申报详情
     */
    Map<String, Object> getApplicationDetail(Long id);

    /**
     * 更新申报信息（仅待审核状态可修改）
     *
     * @param application 申报信息
     */
    void updateApplication(StudentHonorApplication application);

    /**
     * 撤回申报
     *
     * @param id 申报ID
     */
    void withdrawApplication(Long id);

    /**
     * 班级审核
     *
     * @param id       申报ID
     * @param approved 是否通过
     * @param comment  审核意见
     */
    void classReview(Long id, boolean approved, String comment);

    /**
     * 院系审核
     *
     * @param id       申报ID
     * @param approved 是否通过
     * @param comment  审核意见
     */
    void departmentReview(Long id, boolean approved, String comment);

    /**
     * 学校审核
     *
     * @param id       申报ID
     * @param approved 是否通过
     * @param comment  审核意见
     */
    void schoolReview(Long id, boolean approved, String comment);

    /**
     * 批量审核
     *
     * @param ids      申报ID列表
     * @param level    审核级别 (class/department/school)
     * @param approved 是否通过
     * @param comment  审核意见
     * @return 处理数量
     */
    int batchReview(List<Long> ids, String level, boolean approved, String comment);

    /**
     * 获取学生的申报列表
     *
     * @param studentId 学生ID
     * @param periodId  周期ID（可选）
     * @return 申报列表
     */
    List<Map<String, Object>> getStudentApplications(Long studentId, Long periodId);

    /**
     * 获取待审核列表
     *
     * @param reviewLevel 审核级别
     * @param reviewerId  审核人ID
     * @return 待审核列表
     */
    List<Map<String, Object>> getPendingReviewList(String reviewLevel, Long reviewerId);

    /**
     * 检查是否可申报
     *
     * @param studentId   学生ID
     * @param honorTypeId 荣誉类型ID
     * @param periodId    周期ID
     * @return 检查结果 {canApply, reason}
     */
    Map<String, Object> checkCanApply(Long studentId, Long honorTypeId, Long periodId);

    /**
     * 获取可申报的荣誉类型
     *
     * @param studentId 学生ID
     * @param periodId  周期ID
     * @return 荣誉类型列表
     */
    List<Map<String, Object>> getAvailableHonorTypes(Long studentId, Long periodId);

    /**
     * 获取周期内的审核统计
     *
     * @param periodId 周期ID
     * @return 统计数据
     */
    Map<String, Object> getReviewStatistics(Long periodId);

    /**
     * 导出申报数据
     *
     * @param query 查询条件
     * @return 导出数据
     */
    List<Map<String, Object>> exportApplications(Map<String, Object> query);
}
