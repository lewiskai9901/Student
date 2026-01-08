package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.MajorCreateRequest;
import com.school.management.dto.MajorQueryRequest;
import com.school.management.dto.MajorUpdateRequest;
import com.school.management.entity.Major;

import java.util.List;

/**
 * 专业服务接口
 */
public interface MajorService {

    /**
     * 分页查询专业列表
     */
    IPage<Major> getMajorPage(MajorQueryRequest request);

    /**
     * 根据部门ID查询专业列表
     */
    List<Major> getMajorsByDepartmentId(Long departmentId);

    /**
     * 查询所有启用的专业
     */
    List<Major> getAllEnabledMajors();

    /**
     * 根据ID查询专业详情
     */
    Major getMajorById(Long id);

    /**
     * 创建专业
     */
    void createMajor(MajorCreateRequest request);

    /**
     * 更新专业
     */
    void updateMajor(Long id, MajorUpdateRequest request);

    /**
     * 删除专业
     */
    void deleteMajor(Long id);

    /**
     * 批量删除专业
     */
    void batchDeleteMajors(List<Long> ids);
}
