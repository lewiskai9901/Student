package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Map;

/**
 * 组织统计 DTO — 多维度成员计数
 */
@Data
public class OrgStatisticsDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgUnitId;

    /** 归属成员数（primary_org_unit_id = orgUnitId） */
    private long belongingCount;

    /** 岗位人员数（通过 user_positions 任命到本组织的人，含外部兼任） */
    private long staffCount;

    /** 按用户类型细分的归属成员数 { "TEACHER": 5, "STUDENT": 30 } */
    private Map<String, Long> countByUserType;
}
