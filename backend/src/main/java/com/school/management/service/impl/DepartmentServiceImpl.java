package com.school.management.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.DepartmentCreateRequest;
import com.school.management.dto.DepartmentResponse;
import com.school.management.entity.Department;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Override
    @CacheEvict(value = {"departments", "department"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Long createDepartment(DepartmentCreateRequest request) {
        log.info("创建部门: {}", request.getDeptCode());

        // 检查部门编码是否存在
        if (existsDeptCode(request.getDeptCode(), null)) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "部门编码已存在");
        }

        // 创建部门
        Department department = new Department();
        department.setDeptName(request.getDeptName());
        department.setDeptCode(request.getDeptCode());
        department.setDeptDesc(request.getDeptDesc());
        department.setParentId(request.getParentId());
        department.setLeaderId(request.getLeaderId());
        department.setPhone(request.getPhone());
        department.setEmail(request.getEmail());
        department.setAddress(request.getAddress());
        department.setSortOrder(request.getSortOrder());
        department.setStatus(request.getStatus());

        // 设置部门层级和路径
        if (request.getParentId() != null) {
            Department parent = departmentMapper.selectById(request.getParentId());
            if (parent != null) {
                department.setDeptLevel(parent.getDeptLevel() + 1);
                department.setDeptPath(parent.getDeptPath() + "/" + request.getDeptCode());
            } else {
                department.setDeptLevel(1);
                department.setDeptPath("/" + request.getDeptCode());
            }
        } else {
            department.setDeptLevel(1);
            department.setDeptPath("/" + request.getDeptCode());
        }

        departmentMapper.insert(department);

        log.info("部门创建成功: {} - {}", department.getId(), request.getDeptCode());
        return department.getId();
    }

    @Override
    @CacheEvict(value = {"departments", "department"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartment(Long id, DepartmentCreateRequest request) {
        log.info("更新部门: {}", id);

        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }

        // 检查部门编码是否重复
        if (!department.getDeptCode().equals(request.getDeptCode())
                && existsDeptCode(request.getDeptCode(), id)) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "部门编码已存在");
        }

        // 更新部门信息
        department.setDeptName(request.getDeptName());
        department.setDeptCode(request.getDeptCode());
        department.setDeptDesc(request.getDeptDesc());
        department.setParentId(request.getParentId());
        department.setLeaderId(request.getLeaderId());
        department.setPhone(request.getPhone());
        department.setEmail(request.getEmail());
        department.setAddress(request.getAddress());
        department.setSortOrder(request.getSortOrder());
        department.setStatus(request.getStatus());
        department.setUpdatedAt(LocalDateTime.now());

        // 重新设置部门层级和路径
        if (request.getParentId() != null) {
            Department parent = departmentMapper.selectById(request.getParentId());
            if (parent != null) {
                department.setDeptLevel(parent.getDeptLevel() + 1);
                department.setDeptPath(parent.getDeptPath() + "/" + request.getDeptCode());
            }
        } else {
            department.setDeptLevel(1);
            department.setDeptPath("/" + request.getDeptCode());
        }

        departmentMapper.updateById(department);

        log.info("部门更新成功: {}", id);
    }

    @Override
    @CacheEvict(value = {"departments", "department"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        log.info("删除部门: {}", id);

        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }

        // 检查是否有子部门
        List<DepartmentResponse> children = departmentMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "存在子部门，无法删除");
        }

        // 使用MyBatis Plus的逻辑删除(@TableLogic注解)
        int result = departmentMapper.deleteById(id);
        if (result == 0) {
            throw new BusinessException(ResultCode.DATA_OPERATION_ERROR, "删除失败");
        }

        log.info("部门删除成功: {}", id);
    }

    @Override
    @Cacheable(value = "department", key = "#id")
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        log.debug("从数据库查询部门: {}", id);
        List<DepartmentResponse> departments = departmentMapper.selectAllDepartments();
        return departments.stream()
                .filter(dept -> dept.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Cacheable(value = "department", key = "'code:' + #deptCode")
    @Transactional(readOnly = true)
    public Department getDepartmentByCode(String deptCode) {
        log.debug("从数据库查询部门(按编码): {}", deptCode);
        return departmentMapper.selectByDeptCode(deptCode);
    }

    @Override
    @Transactional(readOnly = true)
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<DepartmentResponse> page(
            Integer pageNum, Integer pageSize, String deptName, String deptCode, Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DepartmentResponse> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);

        // 查询所有部门
        List<DepartmentResponse> allDepartments = departmentMapper.selectAllDepartments();

        // 过滤条件
        List<DepartmentResponse> filtered = allDepartments.stream()
            .filter(dept -> deptName == null || dept.getDeptName().contains(deptName))
            .filter(dept -> deptCode == null || dept.getDeptCode().contains(deptCode))
            .filter(dept -> status == null || status.equals(dept.getStatus()))
            .collect(Collectors.toList());

        // 分页
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, filtered.size());
        List<DepartmentResponse> pageData = start < filtered.size() ?
            filtered.subList(start, end) : new ArrayList<>();

        page.setRecords(pageData);
        page.setTotal(filtered.size());
        page.setCurrent(pageNum);
        page.setSize(pageSize);
        page.setPages((filtered.size() + pageSize - 1) / pageSize);

        return page;
    }

    @Override
    @Cacheable(value = "departments", key = "'tree'")
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentTree() {
        log.debug("从数据库查询部门树");
        List<DepartmentResponse> allDepartments = departmentMapper.selectAllDepartments();
        return buildDepartmentTree(allDepartments);
    }

    @Override
    @Cacheable(value = "departments", key = "'all'")
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllEnabledDepartments() {
        log.debug("从数据库查询所有启用的部门");
        return departmentMapper.selectAllEnabled();
    }

    @Override
    @Cacheable(value = "departments", key = "'parent:' + #parentId")
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentsByParentId(Long parentId) {
        log.debug("从数据库查询子部门(父部门ID: {})", parentId);
        return departmentMapper.selectByParentId(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsDeptCode(String deptCode, Long excludeId) {
        return departmentMapper.countByDeptCode(deptCode, excludeId) > 0;
    }

    @Override
    @CacheEvict(value = {"departments", "department"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("更新部门状态: {} -> {}", id, status);

        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }

        department.setStatus(status);
        department.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(department);

        log.info("部门状态更新成功: {}", id);
    }

    /**
     * 构建部门树形结构
     */
    private List<DepartmentResponse> buildDepartmentTree(List<DepartmentResponse> departments) {
        Map<Long, List<DepartmentResponse>> parentChildrenMap = departments.stream()
                .filter(dept -> dept.getParentId() != null && dept.getParentId() != 0)
                .collect(Collectors.groupingBy(DepartmentResponse::getParentId));

        // 设置每个部门的子部门
        departments.forEach(dept -> {
            List<DepartmentResponse> children = parentChildrenMap.get(dept.getId());
            dept.setChildren(children != null ? children : new ArrayList<>());
        });

        // 返回顶级部门（parentId为null或0的部门）
        return departments.stream()
                .filter(dept -> dept.getParentId() == null || dept.getParentId() == 0)
                .collect(Collectors.toList());
    }
}