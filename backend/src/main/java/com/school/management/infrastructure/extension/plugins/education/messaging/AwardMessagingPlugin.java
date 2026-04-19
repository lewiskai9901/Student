package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教育行业奖励表彰事件 — 三好学生/荣誉/表扬/全勤/先进个人/先进班级等.
 */
@Component
public class AwardMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "award"; }
    @Override public String getDomainName() { return "奖励"; }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("HONOR", "荣誉称号",
                "AWARD", "奖励", "POSITIVE", "Trophy", "#22c55e",
                List.of("USER", "ORG_UNIT"), "个人或组织获得荣誉称号"),
            new EventTypeDef("PRAISE", "表扬",
                "AWARD", "奖励", "POSITIVE", "ThumbsUp", "#22c55e",
                List.of("USER"), "教师或管理者的公开表扬"),
            new EventTypeDef("EXEMPLARY", "先进个人",
                "AWARD", "奖励", "POSITIVE", "Star", "#22c55e",
                List.of("USER"), "评选为先进个人"),
            new EventTypeDef("EXEMPLARY_ORG", "先进组织",
                "AWARD", "奖励", "POSITIVE", "Award", "#22c55e",
                List.of("ORG_UNIT"), "评选为先进班级/先进集体"),
            new EventTypeDef("FULL_ATTENDANCE", "全勤奖",
                "AWARD", "奖励", "POSITIVE", "CheckCircle", "#22c55e",
                List.of("USER"), "学期全勤奖励"),
            new EventTypeDef("DISCIPLINE_EXCELLENT", "纪律优秀",
                "AWARD", "奖励", "POSITIVE", "Shield", "#22c55e",
                List.of("USER", "ORG_UNIT"), "纪律考核优秀")
        );
    }
}
