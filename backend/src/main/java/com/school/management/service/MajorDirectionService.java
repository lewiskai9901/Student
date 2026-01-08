package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.entity.MajorDirection;

import java.util.List;

/**
 * 专业方向服务接口
 */
public interface MajorDirectionService {

    /**
     * 分页查询专业方向列表
     */
    IPage<MajorDirection> getDirectionPage(int pageNum, int pageSize, MajorDirection params);

    /**
     * 根据专业ID查询专业方向列表
     */
    List<MajorDirection> getDirectionsByMajorId(Long majorId);

    /**
     * 查询所有专业方向
     */
    List<MajorDirection> getAllDirections();

    /**
     * 根据ID查询专业方向详情
     */
    MajorDirection getDirectionById(Long id);

    /**
     * 创建专业方向
     */
    MajorDirection createDirection(MajorDirection direction);

    /**
     * 更新专业方向
     */
    void updateDirection(Long id, MajorDirection direction);

    /**
     * 删除专业方向
     */
    void deleteDirection(Long id);

    /**
     * 批量删除专业方向
     */
    void batchDeleteDirections(List<Long> ids);

    /**
     * 根据专业ID删除所有方向
     */
    void deleteDirectionsByMajorId(Long majorId);
}
