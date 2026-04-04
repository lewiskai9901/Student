package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("insp_grade_definitions")
public class GradeDefinitionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long gradeSchemeId;
    private String code;
    private String name;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String color;
    private String icon;
    private Integer sortOrder;
}
