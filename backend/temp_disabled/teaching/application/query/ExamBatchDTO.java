package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试批次查询DTO
 */
@Data
@Builder
public class ExamBatchDTO {

    private Long id;

    private Long semesterId;

    private String semesterName;

    private String batchName;

    /**
     * 考试类型: 1-期中考试 2-期末考试 3-补考 4-结业考试
     */
    private Integer examType;

    private String examTypeName;

    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * 状态: 0-草稿 1-已发布 2-进行中 3-已结束
     */
    private Integer status;

    private String statusName;

    private String remark;

    private Long createdBy;

    private LocalDateTime createdAt;

    /**
     * 考试安排列表
     */
    private List<ExamArrangementDTO> arrangements;

    /**
     * 获取考试类型名称
     */
    public static String getExamTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "期中考试";
            case 2 -> "期末考试";
            case 3 -> "补考";
            case 4 -> "结业考试";
            default -> "";
        };
    }

    /**
     * 获取状态名称
     */
    public static String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已发布";
            case 2 -> "进行中";
            case 3 -> "已结束";
            default -> "";
        };
    }
}
