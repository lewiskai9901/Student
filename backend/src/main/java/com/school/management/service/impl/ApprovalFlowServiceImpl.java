package com.school.management.service.impl;

import com.school.management.entity.AppealApprovalRecord;
import com.school.management.entity.User;
import com.school.management.service.ApprovalFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    @Override
    public List<AppealApprovalRecord> initApprovalFlow(Long appealId, Long appealConfigId) {
        log.info("初始化审批流: appealId={}, appealConfigId={}", appealId, appealConfigId);
        return new ArrayList<>();
    }

    @Override
    public List<User> resolveApprovers(String approverType, String approverRoleCode, Long approverUserId, String approverDeptCode, Long gradeId) {
        log.info("解析审批人: type={}", approverType);
        return new ArrayList<>();
    }

    @Override
    public boolean submitToNextStep(Long appealId, Integer currentStep) {
        log.info("提交到下一步骤: appealId={}, currentStep={}", appealId, currentStep);
        return true;
    }

    @Override
    public int checkTimeout() {
        return 0;
    }

    @Override
    public boolean sendReminder(Long recordId) {
        log.info("发送提醒: recordId={}", recordId);
        return true;
    }

    @Override
    public boolean processTimeout(Long recordId) {
        log.info("处理超时: recordId={}", recordId);
        return true;
    }

    @Override
    public boolean checkTriggerCondition(String triggerCondition, Long appealId) {
        return true;
    }

    @Override
    public User resolveGradeDirector(Long gradeId) {
        return null;
    }

    @Override
    public List<User> resolveRoleApprovers(String roleCode) {
        return new ArrayList<>();
    }

    @Override
    public User resolveDeptLeader(String deptCode) {
        return null;
    }
}
