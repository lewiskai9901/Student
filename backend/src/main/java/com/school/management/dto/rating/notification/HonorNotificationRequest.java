package com.school.management.dto.rating.notification;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 荣誉通报生成请求DTO
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class HonorNotificationRequest {

    @NotNull(message = "检查计划ID不能为空")
    private Long checkPlanId;

    @NotNull(message = "评级规则ID不能为空")
    private Long ruleId;

    @NotEmpty(message = "等级ID列表不能为空")
    private List<Long> levelIds;        // 包含的等级

    // 统计周期
    @NotNull(message = "开始日期不能为空")
    private LocalDate periodStart;

    @NotNull(message = "结束日期不能为空")
    private LocalDate periodEnd;

    private String periodLabel;         // 如"2025年12月"

    // 展示选项
    private Boolean showFrequency;      // 显示获得次数
    private Boolean showRate;           // 显示获得率
    private Boolean showConsecutive;    // 显示连续记录
    private Boolean showRanking;        // 显示排名
    private Boolean showDepartmentStat; // 显示院系统计
    private Boolean showTrendChart;     // 显示趋势图表

    // 输出格式
    @NotNull(message = "输出格式不能为空")
    private String outputFormat;        // WORD/PDF/EXCEL/IMAGE

    private String templateType;        // SIMPLE/STANDARD/DETAILED/CERTIFICATE/POSTER

    // 通报标题（可选，为空则自动生成）
    private String notificationTitle;

    // 落款单位
    private String issuerName;          // 如"学生处"

    // 是否直接保存到通报记录
    private Boolean saveToRecord;
}
