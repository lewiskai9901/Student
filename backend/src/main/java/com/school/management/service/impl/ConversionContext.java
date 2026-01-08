package com.school.management.service.impl;

import com.school.management.entity.Class;
import com.school.management.entity.DailyCheckDetail;
import com.school.management.entity.User;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查记录转换上下文
 * 用于在转换过程中共享数据，避免重复查询
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class ConversionContext {
    /**
     * 班级信息映射 (classId -> Class)
     */
    private Map<Long, Class> classMap = new HashMap<>();

    /**
     * 班主任信息映射 (teacherId -> User)
     */
    private Map<Long, User> teacherMap = new HashMap<>();

    /**
     * 量化类型名称缓存 (categoryId -> categoryName)
     */
    private Map<Long, String> categoryNameCache = new HashMap<>();

    /**
     * 按班级分组的扣分明细 (classId -> List<DailyCheckDetail>)
     */
    private Map<Long, List<DailyCheckDetail>> classDetailsMap = new HashMap<>();

    /**
     * 检查涉及的所有班级ID
     */
    private java.util.Set<Long> classIds = new java.util.HashSet<>();
}
