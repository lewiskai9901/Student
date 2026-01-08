package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.HonorType;

import java.util.List;
import java.util.Map;

/**
 * 荣誉类型服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface HonorTypeService extends IService<HonorType> {

    /**
     * 分页查询荣誉类型
     */
    Page<HonorType> pageHonorTypes(Page<?> page, Map<String, Object> query);

    /**
     * 创建荣誉类型
     */
    Long createHonorType(HonorType honorType);

    /**
     * 更新荣誉类型
     */
    void updateHonorType(HonorType honorType);

    /**
     * 删除荣誉类型
     */
    void deleteHonorType(Long id);

    /**
     * 获取荣誉类型详情
     */
    Map<String, Object> getHonorTypeDetail(Long id);

    /**
     * 根据类别获取荣誉类型
     */
    List<HonorType> getByCategory(String category);

    /**
     * 获取所有启用的荣誉类型
     */
    List<HonorType> getAllEnabled();

    /**
     * 获取可用的荣誉类型（用于申报选择）
     */
    List<Map<String, Object>> getAvailableTypes();

    /**
     * 检查类型编码是否存在
     */
    boolean existsByCode(String typeCode, Long excludeId);
}
