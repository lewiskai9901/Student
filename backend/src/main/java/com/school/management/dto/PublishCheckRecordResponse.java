package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布检查记录响应DTO
 *
 * @author system
 * @since 3.1.0
 */
@Data
@Schema(description = "发布检查记录响应")
public class PublishCheckRecordResponse {

    @Schema(description = "检查记录ID")
    private Long recordId;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "发布人ID")
    private Long publisherId;

    @Schema(description = "发布人姓名")
    private String publisherName;

    @Schema(description = "评级配置ID")
    private Long configId;

    @Schema(description = "评级配置名称")
    private String configName;

    @Schema(description = "参与班级数")
    private Integer totalClasses;

    @Schema(description = "评级结果列表")
    private List<RatingResultDTO> ratingResults;

    /**
     * 评级结果DTO
     */
    @Data
    @Schema(description = "评级结果")
    public static class RatingResultDTO {

        @Schema(description = "班级ID")
        private Long classId;

        @Schema(description = "班级名称")
        private String className;

        @Schema(description = "总扣分")
        private BigDecimal totalScore;

        @Schema(description = "排名")
        private Integer ranking;

        @Schema(description = "评级等级")
        private String levelName;

        @Schema(description = "等级颜色")
        private String levelColor;

        @Schema(description = "百分比排名")
        private BigDecimal percentageRank;

        @Schema(description = "奖励积分")
        private Integer rewardPoints;
    }
}
