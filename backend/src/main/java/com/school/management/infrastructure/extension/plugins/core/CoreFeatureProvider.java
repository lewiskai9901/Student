package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.FeatureDef;
import com.school.management.infrastructure.extension.FeatureDefProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 核心(通用)特性定义 —— 不绑任何行业, 所有插件类型均可使用。
 * 涵盖: 用户通用能力 / 场所能力 / 组织能力。
 */
@Component
public class CoreFeatureProvider implements FeatureDefProvider {

    @Override
    public String industry() { return "CORE"; }

    @Override
    public List<FeatureDef> features() {
        return List.of(
            // 用户 / 通用
            new FeatureDef("canLogin", "可登录系统", "该类型用户能否登录系统。与账户状态(status)共同决定:关闭后,此类用户即使有账号、密码正确也无法登录(如访客)。"),
            new FeatureDef("isStaff", "教职工身份", "标记为内部教职工。影响\"教职工\"维度的统计、筛选与部分管理界面的可见性。"),
            new FeatureDef("isExternal", "外部人员", "非本组织正式成员(访客/家长/外包)。通常不计入成员统计,默认权限更受限。"),
            new FeatureDef("profileEditableBySelf", "本人可编辑资料", "允许该类型用户自行编辑个人档案字段(否则只能由管理员维护)。"),
            new FeatureDef("canBeAdminOfOrg", "可任组织管理员", "该类型用户可被指派为某组织的管理员(admin 关系),从而管理该组织。"),
            new FeatureDef("canBeResponsibleForPlace", "可作场所责任人", "该类型用户可被指派为场所的责任人。"),
            // 场所
            new FeatureDef("hasCapacity", "有容量上限", "该场所类型有容纳人数/工位上限,可在场所上设置并校验容量。"),
            new FeatureDef("bookable", "可预订", "该场所类型可被预订占用(进入预订流程),如会议室、活动室。"),
            new FeatureDef("assignable", "可分配", "该场所类型可被分配给组织/班级(归属关系),如教学楼分给某学院。"),
            new FeatureDef("occupiable", "可占用", "该场所类型可登记长期占用(如住宿床位、固定工位)。"),
            new FeatureDef("hasGender", "含性别属性", "该类型区分性别(如宿舍按性别),启用性别相关约束。"),
            new FeatureDef("hasOccupancy", "跟踪占用", "该场所跟踪当前占用情况(已用/空闲)。"),
            // 组织
            new FeatureDef("dataPermissionBoundary", "数据权限边界", "该组织是数据权限的边界节点 —— \"本组织及以下\"等范围以它为根划分。"),
            new FeatureDef("inspectionTarget", "可被检查", "该组织可作为检查/评分的对象(出现在检查任务的可选范围里)。"),
            new FeatureDef("memberManagement", "管理成员", "该组织直接管理成员名册(如班级、部门),可在其下增删成员。"),
            new FeatureDef("attendance", "启用考勤", "该组织启用考勤功能。"),
            new FeatureDef("scheduling", "启用排课", "该组织启用排课/课表功能。")
        );
    }
}
