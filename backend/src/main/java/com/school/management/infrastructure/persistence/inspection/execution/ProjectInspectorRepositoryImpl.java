package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.execution.InspectorRole;
import com.school.management.domain.inspection.model.execution.ProjectInspector;
import com.school.management.domain.inspection.repository.ProjectInspectorRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProjectInspectorRepositoryImpl implements ProjectInspectorRepository {

    private final ProjectInspectorMapper mapper;

    public ProjectInspectorRepositoryImpl(ProjectInspectorMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ProjectInspector save(ProjectInspector inspector) {
        ProjectInspectorPO po = toPO(inspector);
        if (inspector.getId() == null) {
            mapper.insert(po);
            inspector.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return inspector;
    }

    @Override
    public Optional<ProjectInspector> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ProjectInspector> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ProjectInspector> findByProjectIdAndRole(Long projectId, InspectorRole role) {
        return mapper.findByProjectIdAndRole(projectId, role.name()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ProjectInspector> findByUserId(Long userId) {
        return mapper.findByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        mapper.delete(new LambdaQueryWrapper<ProjectInspectorPO>().eq(ProjectInspectorPO::getProjectId, projectId));
    }

    @Override
    public Map<Long, Integer> countByProjectIds(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Integer> map = new HashMap<>();
        for (ProjectInspectorCountRow row : mapper.countByProjectIds(projectIds)) {
            map.put(row.getProjectId(), row.getInspectorCount() == null ? 0 : row.getInspectorCount());
        }
        return map;
    }

    private ProjectInspectorPO toPO(ProjectInspector d) {
        ProjectInspectorPO po = new ProjectInspectorPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setUserId(d.getUserId());
        po.setUserName(d.getUserName());
        po.setRole(d.getRole() != null ? d.getRole().name() : null);
        po.setIsActive(d.getIsActive());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ProjectInspector toDomain(ProjectInspectorPO po) {
        return ProjectInspector.reconstruct(ProjectInspector.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .userId(po.getUserId())
                .userName(po.getUserName())
                .role(po.getRole() != null ? InspectorRole.valueOf(po.getRole()) : null)
                .isActive(po.getIsActive())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
