package com.school.management.service;

import com.school.management.entity.AppealApprovalRecord;
import com.school.management.entity.User;

import java.util.List;

/**
 * 审批流服务接口
 * 核心业务:审批人解析、审批流程控制
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
public interface ApprovalFlowService {

    /**
     * 初始化审批流
     * 根据申诉配置创建所有审批步骤记录
     *
     * @param appealId 申诉ID
     * @param appealConfigId 申诉配置ID
     * @return 审批记录列表
     */
    List<AppealApprovalRecord> initApprovalFlow(Long appealId, Long appealConfigId);

    /**
     * 解析审批人
     * 根据审批人类型(GRADE_DIRECTOR/ROLE/USER/DEPT_LEADER)解析实际的审批人
     *
     * @param approverType 审批人类型
     * @param approverRoleCode 角色编码(type=ROLE时)
     * @param approverUserId 用户ID(type=USER时)
     * @param approverDeptCode 部门编码(type=DEPT_LEADER时)
     * @param gradeId 年级ID(type=GRADE_DIRECTOR时)
     * @return 审批人列表
     */
    List<User> resolveApprovers(
            String approverType,
            String approverRoleCode,
            Long approverUserId,
            String approverDeptCode,
            Long gradeId
    );

    /**
     * 提交到下一审批步骤
     *
     * @param appealId 申诉ID
     * @param currentStep 当前步骤
     * @return 是否成功
     */
    boolean submitToNextStep(Long appealId, Integer currentStep);

    /**
     * 检查审批超时
     * 定时任务调用,处理超时的审批
     *
     * @return 超时记录数量
     */
    int checkTimeout();

    /**
     * 发送审批提醒
     *
     * @param recordId 审批记录ID
     * @return 是否成功
     */
    boolean sendReminder(Long recordId);

    /**
     * 处理审批超时
     * 根据超时动作(REMIND/ESCALATE/AUTO_PASS/AUTO_REJECT)处理
     *
     * @param recordId 审批记录ID
     * @return 是否成功
     */
    boolean processTimeout(Long recordId);

    /**
     * 检查触发条件是否满足
     *
     * @param triggerCondition 触发条件JSON
     * @param appealId 申诉ID
     * @return 是否满足条件
     */
    boolean checkTriggerCondition(String triggerCondition, Long appealId);

    /**
     * 解析年级主任
     *
     * @param gradeId 年级ID
     * @return 年级主任User对象
     */
    User resolveGradeDirector(Long gradeId);

    /**
     * 解析角色审批人
     *
     * @param roleCode 角色编码
     * @return 审批人列表
     */
    List<User> resolveRoleApprovers(String roleCode);

    /**
     * 解析部门负责人
     *
     * @param deptCode 部门编码
     * @return 部门负责人User对象
     */
    User resolveDeptLeader(String deptCode);
}
