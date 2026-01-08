package com.school.management.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 日常检查打分请求
 *
 * @author system
 * @since 1.0.6
 */
@Data
public class DailyScoringRequest {

    /**
     * 检查ID
     */
    @NotNull(message = "检查ID不能为空")
    private Long checkId;

    /**
     * 检查员ID
     */
    @NotNull(message = "检查员ID不能为空")
    private Long checkerId;

    /**
     * 检查员姓名
     */
    private String checkerName;

    /**
     * 扣分明细列表
     * 允许空列表,表示清空所有扣分项
     */
    @Valid
    @NotNull(message = "扣分明细列表不能为null")
    private List<ScoringDetailRequest> details;

    /**
     * 数据版本号（用于乐观锁并发控制）
     * 前端获取数据时返回的version字段值
     */
    private Integer version;
}
