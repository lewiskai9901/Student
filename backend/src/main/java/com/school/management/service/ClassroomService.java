package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.Classroom;

/**
 * 教室服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface ClassroomService {

    /**
     * 分页查询教室
     *
     * @param page 分页参数
     * @param buildingId 教学楼ID
     * @param floor 楼层
     * @param classroomType 教室类型
     * @param status 状态
     * @return 教室分页列表
     */
    IPage<Classroom> page(Page<Classroom> page, Long buildingId, Integer floor,
                         String classroomType, Integer status);

    /**
     * 根据ID获取教室
     *
     * @param id 教室ID
     * @return 教室
     */
    Classroom getById(Long id);

    /**
     * 创建教室
     *
     * @param classroom 教室信息
     * @return 教室
     */
    Classroom create(Classroom classroom);

    /**
     * 更新教室
     *
     * @param id 教室ID
     * @param classroom 教室信息
     * @return 教室
     */
    Classroom update(Long id, Classroom classroom);

    /**
     * 删除教室
     *
     * @param id 教室ID
     */
    void delete(Long id);

    /**
     * 关联班级到教室
     *
     * @param id 教室ID
     * @param classId 班级ID
     * @return 教室
     */
    Classroom assignClass(Long id, Long classId);
}
