package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查评级结果实体类
 * 注意: 该表没有deleted字段,所以不继承BaseEntity
 *
 * @author system
 * @since 3.1.0
 */
@Data
@TableName("check_rating_results")
public class CheckRatingResult {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 评级配置ID
     */
    private Long configId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 排名
     */
    private Integer ranking;

    /**
     * 总班级数
     */
    private Integer totalClasses;

    /**
     * 评级等级ID
     */
    private Long levelId;

    /**
     * 等级名称
     */
    private String levelName;

    /**
     * 百分比排名
     */
    private BigDecimal percentageRank;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
