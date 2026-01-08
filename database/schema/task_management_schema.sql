-- =====================================================
-- 任务管理模块 - 数据库表结构
-- 版本: 1.0
-- 创建日期: 2025-12-27
-- 说明: 与Flowable工作流集成的任务管理系统
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第一部分: 流程模板管理
-- =====================================================

-- ---------------------------------------------------
-- 1.1 流程模板表 (存储自定义的审批流程)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `workflow_templates`;
CREATE TABLE `workflow_templates` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码(唯一)',
    `template_type` VARCHAR(30) NOT NULL DEFAULT 'TASK' COMMENT '模板类型: TASK-任务审批, LEAVE-请假, OTHER-其他',
    `description` VARCHAR(500) COMMENT '模板描述',
    `process_definition_id` VARCHAR(64) COMMENT 'Flowable流程定义ID',
    `process_definition_key` VARCHAR(64) COMMENT 'Flowable流程定义Key',
    `bpmn_xml` LONGTEXT COMMENT 'BPMN流程定义XML',
    `form_config` JSON COMMENT '表单配置(JSON格式)',
    `node_config` JSON COMMENT '节点配置(审批人规则等)',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模板: 0-否, 1-是',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_by` BIGINT COMMENT '创建人ID',
    `created_by_name` VARCHAR(50) COMMENT '创建人姓名',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    INDEX `idx_template_type` (`template_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_is_default` (`is_default`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程模板表';

-- =====================================================
-- 第二部分: 任务管理核心表
-- =====================================================

-- ---------------------------------------------------
-- 2.1 任务主表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks` (
    `id` BIGINT NOT NULL COMMENT '任务ID',
    `task_code` VARCHAR(50) NOT NULL COMMENT '任务编号(如: TASK-20251227-0001)',
    `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
    `description` TEXT COMMENT '任务描述',
    `priority` TINYINT DEFAULT 2 COMMENT '优先级: 1-紧急, 2-普通, 3-低',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-已打回, 5-已取消, 6-审批中',

    -- 分配相关
    `assigner_id` BIGINT NOT NULL COMMENT '分配人ID(创建任务的领导)',
    `assigner_name` VARCHAR(50) COMMENT '分配人姓名',
    `assign_type` TINYINT DEFAULT 1 COMMENT '分配类型: 1-指定个人, 2-批量分配',

    -- 执行人相关(单人任务)
    `assignee_id` BIGINT COMMENT '执行人ID(班主任)',
    `assignee_name` VARCHAR(50) COMMENT '执行人姓名',

    -- 批量分配相关
    `department_id` BIGINT COMMENT '部门ID(批量分配时)',
    `department_name` VARCHAR(100) COMMENT '部门名称',
    `target_ids` JSON COMMENT '目标ID列表(批量分配时的执行人ID数组)',

    -- 时间相关
    `due_date` DATETIME COMMENT '截止时间',
    `accepted_at` DATETIME COMMENT '接收时间',
    `submitted_at` DATETIME COMMENT '提交时间',
    `completed_at` DATETIME COMMENT '完成时间',

    -- 工作流相关
    `workflow_template_id` BIGINT COMMENT '使用的流程模板ID',
    `process_instance_id` VARCHAR(64) COMMENT 'Flowable流程实例ID',
    `current_node` VARCHAR(100) COMMENT '当前审批节点',
    `current_approvers` JSON COMMENT '当前待审批人ID列表',

    -- 附件
    `attachment_ids` JSON COMMENT '任务附件ID列表',

    -- 标准字段
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_code` (`task_code`),
    INDEX `idx_assigner_id` (`assigner_id`),
    INDEX `idx_assignee_id` (`assignee_id`),
    INDEX `idx_department_id` (`department_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_priority` (`priority`),
    INDEX `idx_due_date` (`due_date`),
    INDEX `idx_process_instance_id` (`process_instance_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ---------------------------------------------------
-- 2.2 任务执行人表(支持一个任务多个执行人)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_assignees`;
CREATE TABLE `task_assignees` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `assignee_id` BIGINT NOT NULL COMMENT '执行人ID',
    `assignee_name` VARCHAR(50) COMMENT '执行人姓名',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-已打回',
    `accepted_at` DATETIME COMMENT '接收时间',
    `submitted_at` DATETIME COMMENT '提交时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `process_instance_id` VARCHAR(64) COMMENT '该执行人的流程实例ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_assignee` (`task_id`, `assignee_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_assignee_id` (`assignee_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行人表';

-- ---------------------------------------------------
-- 2.3 任务提交记录表(班主任提交的完成情况)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_submissions`;
CREATE TABLE `task_submissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `task_assignee_id` BIGINT COMMENT '任务执行人记录ID',
    `submitter_id` BIGINT NOT NULL COMMENT '提交人ID',
    `submitter_name` VARCHAR(50) COMMENT '提交人姓名',
    `content` TEXT COMMENT '完成情况说明',
    `attachment_ids` JSON COMMENT '附件ID列表(文件/照片)',
    `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',

    -- 审核相关(最终审核结果)
    `review_status` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-审核中, 2-通过, 3-打回',
    `final_reviewer_id` BIGINT COMMENT '最终审核人ID',
    `final_reviewer_name` VARCHAR(50) COMMENT '最终审核人姓名',
    `final_review_comment` TEXT COMMENT '最终审核意见',
    `final_reviewed_at` DATETIME COMMENT '最终审核时间',

    -- 打回相关
    `reject_count` INT DEFAULT 0 COMMENT '被打回次数',
    `reject_to_node` VARCHAR(100) COMMENT '打回到的节点',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_submitter_id` (`submitter_id`),
    INDEX `idx_review_status` (`review_status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务提交记录表';

-- ---------------------------------------------------
-- 2.4 任务审批记录表(多级审批的每一步记录)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_approval_records`;
CREATE TABLE `task_approval_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `submission_id` BIGINT NOT NULL COMMENT '提交记录ID',
    `process_instance_id` VARCHAR(64) COMMENT 'Flowable流程实例ID',
    `task_definition_key` VARCHAR(100) COMMENT 'Flowable任务定义Key',
    `flowable_task_id` VARCHAR(64) COMMENT 'Flowable任务ID',

    -- 审批节点信息
    `node_name` VARCHAR(100) COMMENT '审批节点名称',
    `node_order` INT DEFAULT 1 COMMENT '审批顺序(第几级)',

    -- 审批人信息
    `approver_id` BIGINT COMMENT '审批人ID',
    `approver_name` VARCHAR(50) COMMENT '审批人姓名',
    `approver_role` VARCHAR(50) COMMENT '审批人角色',

    -- 审批结果
    `approval_status` TINYINT DEFAULT 0 COMMENT '审批状态: 0-待审批, 1-通过, 2-打回, 3-转交',
    `approval_comment` TEXT COMMENT '审批意见',
    `approval_time` DATETIME COMMENT '审批时间',

    -- 打回相关
    `reject_to_node` VARCHAR(100) COMMENT '打回到的节点',
    `reject_reason` TEXT COMMENT '打回原因',

    -- 转交相关
    `transfer_to_id` BIGINT COMMENT '转交给谁',
    `transfer_to_name` VARCHAR(50) COMMENT '转交人姓名',
    `transfer_reason` TEXT COMMENT '转交原因',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_submission_id` (`submission_id`),
    INDEX `idx_approver_id` (`approver_id`),
    INDEX `idx_approval_status` (`approval_status`),
    INDEX `idx_process_instance_id` (`process_instance_id`),
    INDEX `idx_flowable_task_id` (`flowable_task_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务审批记录表';

-- ---------------------------------------------------
-- 2.5 任务操作日志表(审计追踪)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_logs`;
CREATE TABLE `task_logs` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) COMMENT '操作人姓名',
    `action` VARCHAR(50) NOT NULL COMMENT '操作类型: CREATE/ASSIGN/ACCEPT/SUBMIT/APPROVE/REJECT/CANCEL/TRANSFER',
    `action_desc` VARCHAR(200) COMMENT '操作描述',
    `old_status` TINYINT COMMENT '操作前状态',
    `new_status` TINYINT COMMENT '操作后状态',
    `remark` TEXT COMMENT '备注',
    `extra_data` JSON COMMENT '额外数据',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_operator_id` (`operator_id`),
    INDEX `idx_action` (`action`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务操作日志表';

-- =====================================================
-- 第三部分: 站内消息通知
-- =====================================================

-- ---------------------------------------------------
-- 3.1 站内消息表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `system_messages`;
CREATE TABLE `system_messages` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `message_type` VARCHAR(30) NOT NULL COMMENT '消息类型: TASK_ASSIGN-任务分配, TASK_REMIND-任务提醒, TASK_APPROVE-审批通知, SYSTEM-系统通知',
    `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content` TEXT COMMENT '消息内容',
    `sender_id` BIGINT COMMENT '发送人ID(系统消息为空)',
    `sender_name` VARCHAR(50) COMMENT '发送人姓名',
    `receiver_id` BIGINT NOT NULL COMMENT '接收人ID',
    `receiver_name` VARCHAR(50) COMMENT '接收人姓名',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `read_time` DATETIME COMMENT '阅读时间',
    `business_type` VARCHAR(30) COMMENT '业务类型: TASK-任务',
    `business_id` BIGINT COMMENT '业务ID',
    `extra_data` JSON COMMENT '额外数据',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    INDEX `idx_receiver_id` (`receiver_id`),
    INDEX `idx_message_type` (`message_type`),
    INDEX `idx_is_read` (`is_read`),
    INDEX `idx_business` (`business_type`, `business_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息表';

-- =====================================================
-- 第四部分: 初始化数据
-- =====================================================

-- 插入默认任务审批流程模板
INSERT INTO `workflow_templates` (`id`, `template_name`, `template_code`, `template_type`, `description`, `is_default`, `status`, `version`, `sort_order`, `created_by`, `created_by_name`) VALUES
(1, '简单审批(单人)', 'SIMPLE_APPROVE', 'TASK', '任务创建者直接审批，适用于简单任务', 0, 1, 1, 1, 1, '系统'),
(2, '两级审批', 'TWO_LEVEL_APPROVE', 'TASK', '部门负责人审批后，由分管领导终审', 1, 1, 1, 2, 1, '系统'),
(3, '三级审批', 'THREE_LEVEL_APPROVE', 'TASK', '部门负责人→分管领导→校长三级审批', 0, 1, 1, 3, 1, '系统');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 说明:
-- 1. Flowable的流程表(ACT_*)会在应用启动时自动创建
-- 2. 本SQL只创建业务相关的表
-- 3. 流程模板的BPMN XML会通过前端设计器生成后存储
-- =====================================================
