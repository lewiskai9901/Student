package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.PermissionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.PermissionProvider.PermissionDef.of;

/**
 * 通用核心权限 — 平台级,任何部署都需要.
 *
 * 覆盖: system (用户/角色/权限/配置/公告/日志), task, workflow, wechat 等通用模块.
 */
@Component
public class CorePermissionProvider implements PermissionProvider {

    @Override
    public String getModuleCode() { return "core"; }

    @Override
    public String getModuleName() { return "通用核心"; }

    @Override
    public List<PermissionDef> getPermissions() {
        return List.of(
            // ─── system:admin 超级权限 ───
            of("system:admin", "系统管理员", "顶级权限,拥有所有操作"),

            // ─── system:user ───
            of("system:user:view", "查看用户", "查看用户列表/详情"),
            of("system:user:add", "新增用户", ""),
            of("system:user:edit", "编辑用户", ""),
            of("system:user:delete", "删除用户", ""),

            // ─── system:role ───
            of("system:role:view", "查看角色", ""),
            of("system:role:add", "新增角色", ""),
            of("system:role:edit", "编辑角色", ""),
            of("system:role:delete", "删除角色", ""),

            // ─── system:permission ───
            of("system:permission:view", "查看权限", ""),
            of("system:permission:add", "新增权限", ""),
            of("system:permission:edit", "编辑权限", ""),
            of("system:permission:delete", "删除权限", ""),

            // ─── system:config ───
            of("system:config:view", "查看配置", ""),
            of("system:config:add", "新增配置", ""),
            of("system:config:edit", "编辑配置", ""),
            of("system:config:delete", "删除配置", ""),

            // ─── system:announcement ───
            of("system:announcement:view", "查看公告", ""),
            of("system:announcement:add", "发布公告", ""),
            of("system:announcement:edit", "编辑公告", ""),
            of("system:announcement:delete", "删除公告", ""),

            // ─── system:operlog ───
            of("system:operlog:view", "查看操作日志", ""),
            of("system:operlog:delete", "删除操作日志", ""),
            of("system:operlog:clear", "清空操作日志", ""),

            // ─── task ───
            of("task:create", "创建任务", ""),
            of("task:execute", "执行任务", ""),
            of("task:approve", "审批任务", ""),
            of("task:manage", "管理任务", ""),

            // ─── workflow ───
            of("workflow:view", "查看流程", ""),
            of("workflow:create", "创建流程", ""),
            of("workflow:update", "修改流程", ""),
            of("workflow:delete", "删除流程", ""),
            of("workflow:deploy", "部署流程", ""),

            // ─── wechat ───
            of("wechat:push:view", "查看微信推送记录", ""),
            of("wechat:push:send", "发送微信推送", ""),

            // ═══════════════════════════════════════════════════════════════
            // 合并自 CoreLegacyPermissionProvider — 历史 SQL 种子 + @CasbinAccess 补齐
            // ═══════════════════════════════════════════════════════════════

            // ─── dashboard / analytics ───
            of("dashboard:view", "查看首页仪表盘", ""),
            of("analytics:view", "数据分析", ""),

            // ─── asset 资产管理 ───
            of("asset:borrow:create", "创建借用", ""),
            of("asset:borrow:edit", "编辑借用", ""),
            of("asset:borrow:list", "借用管理", ""),
            of("asset:borrow:return", "归还资产", ""),
            of("asset:borrow:view", "查看借用", ""),
            of("asset:category:list", "查看分类", ""),
            of("asset:category:manage", "管理分类", ""),
            of("asset:depreciation:list", "查看折旧记录", ""),
            of("asset:depreciation:manage", "折旧管理", ""),
            of("asset:inventory:create", "资产盘点创建", ""),
            of("asset:inventory:edit", "资产盘点编辑", ""),
            of("asset:inventory:list", "查看盘点", ""),
            of("asset:inventory:view", "资产盘点查看", ""),
            of("asset:list", "查看资产", ""),
            of("asset:manage", "管理资产", ""),
            of("asset:manage:edit", "编辑资产管理", ""),
            of("asset:manage:view", "查看资产管理", ""),
            of("asset:approval:edit", "编辑资产审批", ""),
            of("asset:approval:view", "查看资产审批", ""),

            // ─── calendar / schedule 日程 ───
            // calendar:* / schedule:policy:* 已迁至 EducationPermissionProvider (2026-04-21)
            // 原因: 校历和排班策略是教育场景特有, 不属于通用核心

            // ─── inspection 检查平台 ───
            of("insp:alert:edit", "编辑预警", ""),
            of("insp:alert:manage", "管理预警", ""),
            of("insp:alert:view", "查看预警", ""),
            of("insp:analytics:export", "分析导出", ""),
            of("insp:analytics:manage", "管理分析", ""),
            of("insp:analytics:view", "分析查看", ""),
            of("insp:catalog:create", "创建分类", ""),
            of("insp:catalog:delete", "删除分类", ""),
            of("insp:catalog:edit", "编辑分类", ""),
            of("insp:catalog:manage", "分类管理", ""),
            of("insp:catalog:view", "分类查看", ""),
            of("insp:corrective:create", "创建整改", ""),
            of("insp:corrective:delete", "删除整改", ""),
            of("insp:corrective:execute", "执行整改", ""),
            of("insp:corrective:manage", "整改管理", ""),
            of("insp:corrective:view", "整改查看", ""),
            of("insp:execution:edit", "编辑执行", ""),
            of("insp:execution:view", "查看执行", ""),
            of("insp:iot-sensor:create", "创建 IoT 传感器", ""),
            of("insp:iot-sensor:delete", "删除 IoT 传感器", ""),
            of("insp:iot-sensor:edit", "编辑 IoT 传感器", ""),
            of("insp:iot-sensor:view", "查看 IoT 传感器", ""),
            of("insp:iot-sensor:write-reading", "写入 IoT 读数", ""),
            of("insp:knowledge:create", "创建知识库", ""),
            of("insp:knowledge:manage", "管理知识库", ""),
            of("insp:knowledge:view", "查看知识库", ""),
            of("insp:plan:create", "创建计划", ""),
            of("insp:plan:delete", "删除计划", ""),
            of("insp:plan:edit", "编辑计划", ""),
            of("insp:plan:execute", "执行计划", ""),
            of("insp:plan:view", "查看计划", ""),
            of("insp:platform:manage", "检查平台管理", ""),
            of("insp:platform:view", "检查平台查看", ""),
            of("insp:project:create", "项目创建", ""),
            of("insp:project:delete", "删除项目", ""),
            of("insp:project:edit", "项目编辑", ""),
            of("insp:project:manage", "项目管理", ""),
            of("insp:project:publish", "发布项目", ""),
            of("insp:project:view", "项目查看", ""),
            of("insp:rating:manage", "管理评分", ""),
            of("insp:rating:view", "查看评分", ""),
            of("insp:rating-link:manage", "评级链接管理", ""),
            of("insp:rating-link:view", "评级链接查看", ""),
            of("insp:response-set:create", "创建选项集", ""),
            of("insp:response-set:delete", "删除选项集", ""),
            of("insp:response-set:edit", "编辑选项集", ""),
            of("insp:response-set:manage", "选项集管理", ""),
            of("insp:response-set:view", "选项集查看", ""),
            of("insp:scoring-policy:create", "创建评分策略", ""),
            of("insp:scoring-policy:delete", "删除评分策略", ""),
            of("insp:scoring-policy:edit", "编辑评分策略", ""),
            of("insp:scoring-policy:view", "查看评分策略", ""),
            of("insp:scoring-profile:create", "创建评分配置", ""),
            of("insp:scoring-profile:delete", "删除评分配置", ""),
            of("insp:scoring-profile:edit", "评分配置编辑", ""),
            of("insp:scoring-profile:view", "评分配置查看", ""),
            of("insp:submission:admin", "管理提交", ""),
            of("insp:submission:create", "创建提交", ""),
            of("insp:submission:execute", "执行提交", ""),
            of("insp:submission:view", "查看提交", ""),
            of("insp:sync:access", "离线同步", ""),
            of("insp:task:create", "创建任务", ""),
            of("insp:task:edit", "编辑任务", ""),
            of("insp:task:execute", "任务执行", ""),
            of("insp:task:publish", "发布任务", ""),
            of("insp:task:review", "任务审核", ""),
            of("insp:task:view", "任务查看", ""),
            of("insp:template:create", "模板创建", ""),
            of("insp:template:delete", "模板删除", ""),
            of("insp:template:edit", "模板编辑", ""),
            of("insp:template:publish", "模板发布", ""),
            of("insp:template:view", "模板查看", ""),
            of("insp:violation:create", "创建违规记录", ""),
            of("insp:violation:delete", "删除违规记录", ""),
            of("insp:violation:edit", "编辑违规记录", ""),
            of("insp:violation:view", "查看违规记录", ""),
            of("inspection:appeal:create", "申诉创建", ""),
            of("inspection:appeal:review", "申诉审核", ""),
            of("inspection:appeal:view", "申诉查看", ""),
            of("inspection:export:create", "创建导出", ""),

            // ─── place 场所 ───
            of("place:add", "创建场所", ""),
            of("place:delete", "删除场所", ""),
            of("place:edit", "更新场所", ""),
            of("place:occupancy", "入住管理", ""),
            of("place:view", "查看场所", ""),

            // ─── system 系统管理扩展 ───
            of("system:announcement", "公告管理", ""),
            of("system:audit", "审计日志", ""),
            of("system:audit:view", "查看审计日志", ""),
            of("system:config", "系统配置", ""),
            of("system:department", "部门管理", ""),
            of("system:department:view", "查看部门", ""),
            of("system:manage", "系统管理", ""),
            of("system:operlog", "操作日志", ""),
            of("system:operlog:export", "导出操作日志", ""),
            of("system:org", "组织架构管理", ""),
            of("system:org:create", "创建组织", ""),
            of("system:org:delete", "删除组织", ""),
            of("system:org:edit", "编辑组织类型", ""),
            of("system:org:update", "更新组织", ""),
            of("system:org:view", "查看组织架构", ""),
            of("system:permission", "权限管理", ""),
            of("system:permission:tree", "查看权限树", ""),
            of("system:place", "场所类型管理", ""),
            of("system:place-type:edit", "编辑场所类型", ""),
            of("system:place-type:view", "查看场所类型", ""),
            of("system:place:add", "新增场所类型", ""),
            of("system:place:delete", "删除场所类型", ""),
            of("system:place:edit", "编辑场所类型", ""),
            of("system:place:view", "查看场所类型", ""),
            of("system:role", "角色管理", ""),
            of("system:user", "用户管理", ""),
            of("system:user:reset", "重置密码", ""),

            // ─── task 任务 ───
            of("task:approval", "任务审批", ""),
            of("task:list", "任务列表页", ""),
            of("task:menu", "任务管理", ""),
            of("task:my", "我的任务页", ""),
            of("task:view", "查看任务", ""),
            of("task:workflow:manage", "流程管理权", ""),

            // ─── admin / tenant 租户 ───
            of("admin:access", "管理员访问", ""),
            of("tenant:create", "创建租户", ""),
            of("tenant:delete", "删除租户", ""),
            of("tenant:update", "更新租户", ""),
            of("tenant:view", "查看租户", ""),

            // ─── data_module 数据模块 ───
            of("data_module:create", "创建数据模块", ""),
            of("data_module:delete", "删除数据模块", ""),
            of("data_module:update", "更新数据模块", ""),

            // ─── entity-event 事件系统 ───
            of("entity-event:view", "查看实体事件", ""),
            of("entity-event-type:add", "新增事件类型", ""),
            of("entity-event-type:delete", "删除事件类型", ""),
            of("entity-event-type:edit", "编辑事件类型", ""),
            of("entity-event-type:view", "查看事件类型", ""),
            of("event-trigger:add", "新增事件触发器", ""),
            of("event-trigger:delete", "删除事件触发器", ""),
            of("event-trigger:edit", "编辑事件触发器", ""),
            of("event-trigger:view", "查看事件触发器", ""),

            // ─── msg 消息配置 ───
            of("msg-config:create", "创建消息配置", ""),
            of("msg-config:delete", "删除消息配置", ""),
            of("msg-config:edit", "编辑消息配置", ""),
            of("msg-config:view", "查看消息配置", ""),
            of("msg-notification:delete", "删除通知", ""),
            of("msg-notification:edit", "编辑通知", ""),
            of("msg-notification:view", "查看通知", ""),

            // ─── my:* 自我相关 ───
            of("my:schedule:view", "查看我的课表", ""),
            of("my:substitute:view", "查看我的代课", ""),
            of("my:user_student:view", "查看我的学生", "")

            // ─── quantification 量化考核 ───
            // quantification:* 已迁至 EducationPermissionProvider (2026-04-21)
            // 原因: 量化考核是学生工作场景特有, 不属于通用核心
        );
    }
}
