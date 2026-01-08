package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.DeductionItemCreateRequest;
import com.school.management.dto.DeductionItemUpdateRequest;
import com.school.management.entity.DeductionItem;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.DeductionItemMapper;
import com.school.management.service.DeductionItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 扣分项服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeductionItemServiceImpl extends ServiceImpl<DeductionItemMapper, DeductionItem> implements DeductionItemService {

    private final DeductionItemMapper deductionItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDeductionItem(DeductionItemCreateRequest request) {
        log.info("创建扣分项: {}", request.getItemName());

        DeductionItem deductionItem = new DeductionItem();
        deductionItem.setTypeId(request.getTypeId());
        deductionItem.setItemName(request.getItemName());
        deductionItem.setDeductMode(request.getDeductMode());
        deductionItem.setFixedScore(request.getFixedScore());
        deductionItem.setBaseScore(request.getBaseScore());
        deductionItem.setPerPersonScore(request.getPerPersonScore());
        deductionItem.setRangeConfig(request.getRangeConfig());
        deductionItem.setDescription(request.getDescription());
        deductionItem.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        deductionItem.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        deductionItem.setAllowPhoto(request.getAllowPhoto() != null ? request.getAllowPhoto() : 0);
        deductionItem.setAllowRemark(request.getAllowRemark() != null ? request.getAllowRemark() : 0);
        deductionItem.setAllowStudents(request.getAllowStudents() != null ? request.getAllowStudents() : 0);
        deductionItem.setEnableWeight(request.getEnableWeight() != null ? request.getEnableWeight() : 0);
        deductionItem.setWeightConfigId(request.getWeightConfigId());

        deductionItemMapper.insert(deductionItem);

        log.info("扣分项创建成功: {}", deductionItem.getId());
        return deductionItem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeductionItem(DeductionItemUpdateRequest request) {
        log.info("更新扣分项: {}", request.getId());

        DeductionItem deductionItem = deductionItemMapper.selectById(request.getId());
        if (deductionItem == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "扣分项不存在");
        }

        deductionItem.setTypeId(request.getTypeId());
        deductionItem.setItemName(request.getItemName());
        deductionItem.setDeductMode(request.getDeductMode());
        deductionItem.setFixedScore(request.getFixedScore());
        deductionItem.setBaseScore(request.getBaseScore());
        deductionItem.setPerPersonScore(request.getPerPersonScore());
        deductionItem.setRangeConfig(request.getRangeConfig());
        deductionItem.setDescription(request.getDescription());
        deductionItem.setSortOrder(request.getSortOrder());
        deductionItem.setStatus(request.getStatus());
        deductionItem.setAllowPhoto(request.getAllowPhoto());
        deductionItem.setAllowRemark(request.getAllowRemark());
        deductionItem.setAllowStudents(request.getAllowStudents());
        deductionItem.setEnableWeight(request.getEnableWeight());
        deductionItem.setWeightConfigId(request.getWeightConfigId());
        deductionItem.setUpdatedAt(LocalDateTime.now());

        deductionItemMapper.updateById(deductionItem);

        log.info("扣分项更新成功: {}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeductionItem(Long id) {
        log.info("删除扣分项: {}", id);

        DeductionItem deductionItem = deductionItemMapper.selectById(id);
        if (deductionItem == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "扣分项不存在");
        }

        deductionItemMapper.deleteById(id);

        log.info("扣分项删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeductionItems(List<Long> ids) {
        log.info("批量删除扣分项: {}", ids);

        for (Long id : ids) {
            deleteDeductionItem(id);
        }
    }

    @Override
    public DeductionItem getDeductionItemById(Long id) {
        return deductionItemMapper.selectById(id);
    }

    @Override
    public List<DeductionItem> getDeductionItemsByTypeId(Long typeId) {
        return deductionItemMapper.selectByTypeId(typeId);
    }

    @Override
    public List<DeductionItem> getEnabledDeductionItemsByTypeId(Long typeId) {
        return deductionItemMapper.selectEnabledByTypeId(typeId);
    }

    @Override
    public IPage<DeductionItem> getDeductionItemPage(Integer pageNum, Integer pageSize, Long typeId) {
        Page<DeductionItem> page = new Page<>(pageNum, pageSize);
        return deductionItemMapper.selectDeductionItemPage(page, typeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("更新扣分项状态: {} -> {}", id, status);

        DeductionItem deductionItem = deductionItemMapper.selectById(id);
        if (deductionItem == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "扣分项不存在");
        }

        deductionItem.setStatus(status);
        deductionItem.setUpdatedAt(LocalDateTime.now());
        deductionItemMapper.updateById(deductionItem);

        log.info("扣分项状态更新成功: {}", id);
    }
}
