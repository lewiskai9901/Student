package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 检查字典服务接口 (V3.0)
 * 管理检查类别和检查项
 *
 * @author Claude
 * @since 2025-11-24
 */
public interface CheckDictionaryService {

    // ==================== 检查类别管理 ====================

    /**
     * 创建检查类别
     *
     * @param request 创建请求
     * @return 类别ID
     */
    Long createCheckCategory(Map<String, Object> request);

    /**
     * 更新检查类别
     *
     * @param id 类别ID
     * @param request 更新请求
     */
    void updateCheckCategory(Long id, Map<String, Object> request);

    /**
     * 删除检查类别
     *
     * @param id 类别ID
     */
    void deleteCheckCategory(Long id);

    /**
     * 获取检查类别详情
     *
     * @param id 类别ID
     * @return 类别详情
     */
    Map<String, Object> getCheckCategoryDetail(Long id);

    /**
     * 分页查询检查类别
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<Map<String, Object>> pageCheckCategories(Page<Map<String, Object>> page, Map<String, Object> query);

    /**
     * 查询所有启用的检查类别
     *
     * @return 类别列表
     */
    List<Map<String, Object>> getAllEnabledCategories();

    /**
     * 根据类别类型查询类别
     *
     * @param categoryType 类别类型 (HYGIENE/DISCIPLINE/OTHER)
     * @return 类别列表
     */
    List<Map<String, Object>> getCategoriesByType(String categoryType);

    // ==================== 检查项管理 ====================

    /**
     * 创建检查项
     *
     * @param request 创建请求
     * @return 检查项ID
     */
    Long createCheckItem(Map<String, Object> request);

    /**
     * 更新检查项
     *
     * @param id 检查项ID
     * @param request 更新请求
     */
    void updateCheckItem(Long id, Map<String, Object> request);

    /**
     * 删除检查项
     *
     * @param id 检查项ID
     */
    void deleteCheckItem(Long id);

    /**
     * 批量删除检查项
     *
     * @param ids 检查项ID列表
     */
    void batchDeleteCheckItems(List<Long> ids);

    /**
     * 获取检查项详情
     *
     * @param id 检查项ID
     * @return 检查项详情
     */
    Map<String, Object> getCheckItemDetail(Long id);

    /**
     * 分页查询检查项
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<Map<String, Object>> pageCheckItems(Page<Map<String, Object>> page, Map<String, Object> query);

    /**
     * 根据类别ID查询检查项
     *
     * @param categoryId 类别ID
     * @return 检查项列表
     */
    List<Map<String, Object>> getItemsByCategoryId(Long categoryId);

    /**
     * 查询所有启用的检查项
     *
     * @return 检查项列表
     */
    List<Map<String, Object>> getAllEnabledItems();

    /**
     * 批量导入检查项
     *
     * @param categoryId 类别ID
     * @param items 检查项列表
     * @return 导入数量
     */
    int batchImportCheckItems(Long categoryId, List<Map<String, Object>> items);
}
