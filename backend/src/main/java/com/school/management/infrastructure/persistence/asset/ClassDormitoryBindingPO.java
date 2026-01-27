package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级-宿舍关联持久化对象
 * DDD infrastructure layer replacement for V1 entity.ClassDormitoryBinding
 */
@Data
@TableName("class_dormitory_bindings")
public class ClassDormitoryBindingPO {

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
