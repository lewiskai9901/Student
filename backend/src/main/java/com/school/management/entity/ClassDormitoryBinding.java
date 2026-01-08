package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级-宿舍关联实体类
 *
 * @author system
 * @since 2.0.0
 */
@Data
@TableName("class_dormitory_bindings")
public class ClassDormitoryBinding {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 该宿舍该班学生数
     */
    private Integer studentCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
