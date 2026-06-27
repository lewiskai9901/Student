package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.FeatureDef;
import com.school.management.infrastructure.extension.FeatureDefProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教育行业特性定义 —— 归属 EDU, 只有教育类型可使用 (+ 核心通用特性)。
 * EDU 插件禁用时本 bean 不存在 → 这些特性自动不在注册表 → 核心校验会拒绝任何类型再用它们。
 */
@Component
public class EducationFeatureProvider implements FeatureDefProvider {

    @Override
    public String industry() { return "EDU"; }

    @Override
    public List<FeatureDef> features() {
        return List.of(
            new FeatureDef("isLearner", "学习者", "标记为学生类。可被评教、记成绩、纳入班级名册等学习场景;org-impact 的\"学生数\"按它统计。"),
            new FeatureDef("canEnroll", "可注册入学 / 选课", "参与入学注册与选课流程。"),
            new FeatureDef("canTeach", "可授课", "可被排课、担任任课教师;org-impact 的\"教师数\"按它统计。"),
            new FeatureDef("canCounsel", "可带班 / 辅导", "可担任辅导员、带学生。"),
            new FeatureDef("canApproveGrade", "可审批成绩", "具备成绩审批权限。"),
            new FeatureDef("hasGuardian", "有监护人", "该类型用户关联监护人(家长)信息。"),
            new FeatureDef("receivesPersonalGrade", "接收个人成绩", "会产生/接收个人成绩记录。"),
            new FeatureDef("attendanceTracked", "纳入考勤", "该类型用户参与考勤统计。"),
            new FeatureDef("canBeAssignedToClass", "可分配到班级", "可被编入班级(班级成员)。"),
            new FeatureDef("manageableByOrgAdmin", "组织管理员可管", "可由其所属组织的管理员管理。"),
            new FeatureDef("hasStudents", "含学生", "该组织下挂学生。"),
            new FeatureDef("hasClasses", "含班级", "该组织下设班级。"),
            new FeatureDef("hasExams", "有考试", "该组织组织考试。"),
            new FeatureDef("hasTimetable", "有课表", "该组织有课表。"),
            new FeatureDef("hasAttendance", "有考勤", "该组织有考勤记录。"),
            new FeatureDef("hasProjector", "配备投影仪", "该场所配备投影仪(教室设备)。"),
            new FeatureDef("hasAC", "配备空调", "该场所配备空调。")
        );
    }
}
