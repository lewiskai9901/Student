package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.HonorType;
import com.school.management.entity.evaluation.StudentHonorApplication;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.HonorTypeMapper;
import com.school.management.mapper.evaluation.StudentHonorApplicationMapper;
import com.school.management.service.evaluation.HonorTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 荣誉类型服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HonorTypeServiceImpl extends ServiceImpl<HonorTypeMapper, HonorType>
        implements HonorTypeService {

    private final HonorTypeMapper honorTypeMapper;
    private final StudentHonorApplicationMapper applicationMapper;

    @Override
    public Page<HonorType> pageHonorTypes(Page<?> page, Map<String, Object> query) {
        LambdaQueryWrapper<HonorType> wrapper = new LambdaQueryWrapper<>();

        String typeCode = (String) query.get("typeCode");
        String typeName = (String) query.get("typeName");
        String category = (String) query.get("category");
        Integer status = (Integer) query.get("status");

        wrapper.like(StringUtils.hasText(typeCode), HonorType::getTypeCode, typeCode)
                .like(StringUtils.hasText(typeName), HonorType::getTypeName, typeName)
                .eq(StringUtils.hasText(category), HonorType::getCategory, category)
                .eq(status != null, HonorType::getStatus, status)
                .orderByAsc(HonorType::getSortOrder)
                .orderByDesc(HonorType::getCreatedAt);

        return honorTypeMapper.selectPage((Page<HonorType>) page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHonorType(HonorType honorType) {
        // 检查编码是否存在
        if (existsByCode(honorType.getTypeCode(), null)) {
            throw new BusinessException("荣誉类型编码已存在");
        }

        // 设置默认值
        if (honorType.getStatus() == null) {
            honorType.setStatus(1);
        }
        if (honorType.getSortOrder() == null) {
            honorType.setSortOrder(0);
        }
        if (honorType.getRequiredAttachments() == null) {
            honorType.setRequiredAttachments(0);
        }

        honorTypeMapper.insert(honorType);
        log.info("创建荣誉类型: id={}, code={}", honorType.getId(), honorType.getTypeCode());
        return honorType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHonorType(HonorType honorType) {
        HonorType existing = honorTypeMapper.selectById(honorType.getId());
        if (existing == null) {
            throw new BusinessException("荣誉类型不存在");
        }

        // 检查编码是否存在（排除自己）
        if (honorType.getTypeCode() != null && existsByCode(honorType.getTypeCode(), honorType.getId())) {
            throw new BusinessException("荣誉类型编码已存在");
        }

        honorTypeMapper.updateById(honorType);
        log.info("更新荣誉类型: id={}", honorType.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHonorType(Long id) {
        HonorType existing = honorTypeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("荣誉类型不存在");
        }

        // 检查是否有关联的荣誉申报
        Long applicationCount = applicationMapper.selectCount(
            new LambdaQueryWrapper<StudentHonorApplication>()
                .eq(StudentHonorApplication::getHonorTypeId, id)
        );
        if (applicationCount != null && applicationCount > 0) {
            throw new BusinessException("该荣誉类型已有关联的申报记录，无法删除");
        }

        honorTypeMapper.deleteById(id);
        log.info("删除荣誉类型: id={}", id);
    }

    @Override
    public Map<String, Object> getHonorTypeDetail(Long id) {
        Map<String, Object> detail = honorTypeMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("荣誉类型不存在");
        }
        return detail;
    }

    @Override
    public List<HonorType> getByCategory(String category) {
        return honorTypeMapper.selectByCategory(category);
    }

    @Override
    public List<HonorType> getAllEnabled() {
        return honorTypeMapper.selectAllEnabled();
    }

    @Override
    public List<Map<String, Object>> getAvailableTypes() {
        return honorTypeMapper.selectAvailableTypes();
    }

    @Override
    public boolean existsByCode(String typeCode, Long excludeId) {
        HonorType existing = honorTypeMapper.selectByCode(typeCode);
        if (existing == null) {
            return false;
        }
        return excludeId == null || !existing.getId().equals(excludeId);
    }
}
