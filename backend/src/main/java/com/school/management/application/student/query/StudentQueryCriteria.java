package com.school.management.application.student.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生查询条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentQueryCriteria {

    private String keyword;
    private Long classId;
    private Long orgUnitId;
    private Integer gradeLevel;
    private Integer status;
    private Long dormitoryId;
    private Integer pageNum;
    private Integer pageSize;
}
