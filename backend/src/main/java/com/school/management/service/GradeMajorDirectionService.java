package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.entity.GradeMajorDirection;

import java.util.List;

/**
 * 学年专业方向关联服务接口
 */
public interface GradeMajorDirectionService {

    /**
     * 分页查询学年专业方向关联列表
     */
    IPage<GradeMajorDirection> getGradeMajorDirectionPage(int pageNum, int pageSize, GradeMajorDirection params);

    /**
     * 根据学年查询专业方向列表
     */
    List<GradeMajorDirection> getByAcademicYear(Integer academicYear);

    /**
     * 根据专业方向ID查询关联的学年列表
     */
    List<GradeMajorDirection> getByMajorDirectionId(Long majorDirectionId);

    /**
     * 根据ID查询详情
     */
    GradeMajorDirection getDetailById(Long id);

    /**
     * 根据学年和专业方向ID查询
     */
    GradeMajorDirection getByYearAndDirection(Integer academicYear, Long majorDirectionId);

    /**
     * 为学年添加专业方向
     */
    GradeMajorDirection addDirectionToYear(GradeMajorDirection gradeMajorDirection);

    /**
     * 批量为学年添加专业方向
     */
    void batchAddDirectionsToYear(Integer academicYear, List<Long> majorDirectionIds);

    /**
     * 更新学年专业方向配置
     */
    void updateGradeMajorDirection(Long id, GradeMajorDirection gradeMajorDirection);

    /**
     * 删除学年专业方向关联
     */
    void deleteGradeMajorDirection(Long id);

    /**
     * 批量删除学年专业方向关联
     */
    void batchDeleteGradeMajorDirections(List<Long> ids);
}
