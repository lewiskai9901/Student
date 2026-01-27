package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.exception.BusinessException;
import com.school.management.dto.MajorCreateRequest;
import com.school.management.dto.MajorQueryRequest;
import com.school.management.dto.MajorUpdateRequest;
import com.school.management.entity.Major;
import com.school.management.mapper.MajorMapper;
import com.school.management.service.MajorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 专业服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MajorServiceImpl implements MajorService {

    private final MajorMapper majorMapper;

    @Override
    @Transactional(readOnly = true)
    public IPage<Major> getMajorPage(MajorQueryRequest request) {
        Page<Major> page = new Page<>(request.getPageNum(), request.getPageSize());

        Major params = new Major();
        params.setMajorName(request.getMajorName());
        params.setMajorCode(request.getMajorCode());
        params.setOrgUnitId(request.getOrgUnitId());
        params.setStatus(request.getStatus());

        return majorMapper.selectMajorPage(page, params);
    }

    @Override
    @Cacheable(value = "majors", key = "'orgUnit:' + #orgUnitId")
    @Transactional(readOnly = true)
    public List<Major> getMajorsByOrgUnitId(Long orgUnitId) {
        log.debug("从数据库查询专业(按组织单元ID): {}", orgUnitId);
        return majorMapper.selectByOrgUnitId(orgUnitId);
    }

    @Override
    @Cacheable(value = "majors", key = "'all'")
    @Transactional(readOnly = true)
    public List<Major> getAllEnabledMajors() {
        log.debug("从数据库查询所有启用的专业");
        return majorMapper.selectAllEnabled();
    }

    @Override
    @Cacheable(value = "major", key = "#id")
    @Transactional(readOnly = true)
    public Major getMajorById(Long id) {
        log.debug("从数据库查询专业: {}", id);
        Major major = majorMapper.selectMajorById(id);
        if (major == null) {
            throw new BusinessException("专业不存在");
        }
        return major;
    }

    @Override
    @CacheEvict(value = {"majors", "major"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void createMajor(MajorCreateRequest request) {
        Major major = new Major();
        BeanUtils.copyProperties(request, major);

        // 设置默认值
        if (major.getStatus() == null) {
            major.setStatus(1);
        }

        majorMapper.insert(major);
    }

    @Override
    @CacheEvict(value = {"majors", "major"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updateMajor(Long id, MajorUpdateRequest request) {
        Major existingMajor = majorMapper.selectById(id);
        if (existingMajor == null) {
            throw new BusinessException("专业不存在");
        }

        Major major = new Major();
        BeanUtils.copyProperties(request, major);
        major.setId(id);

        majorMapper.updateById(major);
    }

    @Override
    @CacheEvict(value = {"majors", "major"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteMajor(Long id) {
        Major major = majorMapper.selectById(id);
        if (major == null) {
            throw new BusinessException("专业不存在");
        }

        majorMapper.deleteById(id);
    }

    @Override
    @CacheEvict(value = {"majors", "major"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteMajors(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的专业");
        }

        majorMapper.deleteBatchIds(ids);
    }
}
