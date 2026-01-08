package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.DeductionItemCreateRequest;
import com.school.management.dto.DeductionItemUpdateRequest;
import com.school.management.entity.DeductionItem;

import java.util.List;

/**
 * 扣分项服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface DeductionItemService extends IService<DeductionItem> {

    /**
     * 创建扣分项
     */
    Long createDeductionItem(DeductionItemCreateRequest request);

    /**
     * 更新扣分项
     */
    void updateDeductionItem(DeductionItemUpdateRequest request);

    /**
     * 删除扣分项
     */
    void deleteDeductionItem(Long id);

    /**
     * 批量删除扣分项
     */
    void deleteDeductionItems(List<Long> ids);

    /**
     * 根据ID获取扣分项
     */
    DeductionItem getDeductionItemById(Long id);

    /**
     * 根据类型ID获取扣分项列表
     */
    List<DeductionItem> getDeductionItemsByTypeId(Long typeId);

    /**
     * 根据类型ID获取启用的扣分项列表
     */
    List<DeductionItem> getEnabledDeductionItemsByTypeId(Long typeId);

    /**
     * 分页查询扣分项
     */
    IPage<DeductionItem> getDeductionItemPage(Integer pageNum, Integer pageSize, Long typeId);

    /**
     * 更新状态
     */
    void updateStatus(Long id, Integer status);
}
