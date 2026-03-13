package com.school.management.infrastructure.persistence.space;

import com.school.management.domain.space.model.entity.SpaceClassAssignment;
import com.school.management.domain.space.repository.SpaceClassAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 场所-班级分配仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SpaceClassAssignmentRepositoryImpl implements SpaceClassAssignmentRepository {

    private final SpaceClassAssignmentMapper mapper;

    @Override
    public SpaceClassAssignment save(SpaceClassAssignment assignment) {
        SpaceClassAssignmentPO po = toPO(assignment);
        if (assignment.getId() == null) {
            mapper.insert(po);
            assignment.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return assignment;
    }

    @Override
    public Optional<SpaceClassAssignment> findById(Long id) {
        SpaceClassAssignmentPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<SpaceClassAssignment> findBySpaceId(Long spaceId) {
        return mapper.selectBySpaceIdWithRelations(spaceId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SpaceClassAssignment> findByClassId(Long classId) {
        return mapper.selectByClassIdWithRelations(classId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SpaceClassAssignment> findByOrgUnitId(Long orgUnitId) {
        return mapper.selectByOrgUnitIdWithRelations(orgUnitId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<SpaceClassAssignment> findBySpaceIdAndClassId(Long spaceId, Long classId) {
        SpaceClassAssignmentPO po = mapper.selectBySpaceIdAndClassId(spaceId, classId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteBySpaceId(Long spaceId) {
        mapper.deleteBySpaceId(spaceId);
    }

    @Override
    public void deleteByClassId(Long classId) {
        mapper.deleteByClassId(classId);
    }

    @Override
    public boolean existsBySpaceIdAndClassId(Long spaceId, Long classId) {
        return mapper.checkExists(spaceId, classId) > 0;
    }

    @Override
    public int countBySpaceId(Long spaceId) {
        return mapper.countBySpaceId(spaceId);
    }

    @Override
    public int countByClassId(Long classId) {
        return mapper.countByClassId(classId);
    }

    @Override
    public void batchSave(List<SpaceClassAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        List<SpaceClassAssignmentPO> poList = assignments.stream()
            .map(this::toPO)
            .collect(Collectors.toList());
        mapper.batchInsert(poList);
        // 回填ID
        for (int i = 0; i < assignments.size(); i++) {
            assignments.get(i).setId(poList.get(i).getId());
        }
    }

    // ========== 转换方法 ==========

    private SpaceClassAssignmentPO toPO(SpaceClassAssignment assignment) {
        SpaceClassAssignmentPO po = new SpaceClassAssignmentPO();
        po.setId(assignment.getId());
        po.setSpaceId(assignment.getSpaceId());
        po.setClassId(assignment.getClassId());
        po.setOrgUnitId(assignment.getOrgUnitId());
        po.setAssignedBeds(assignment.getAssignedBeds());
        po.setPriority(assignment.getPriority());
        po.setStatus(assignment.getStatusCode());
        po.setRemark(assignment.getRemark());
        po.setAssignedBy(assignment.getAssignedBy());
        po.setAssignedAt(assignment.getAssignedAt());
        po.setCreatedAt(assignment.getCreatedAt());
        po.setUpdatedAt(assignment.getUpdatedAt());
        return po;
    }

    private SpaceClassAssignment toDomain(SpaceClassAssignmentPO po) {
        return SpaceClassAssignment.reconstitute(
            po.getId(),
            po.getSpaceId(),
            po.getClassId(),
            po.getOrgUnitId(),
            po.getAssignedBeds(),
            po.getPriority(),
            po.getStatus(),
            po.getRemark(),
            po.getAssignedBy(),
            po.getAssignedAt(),
            po.getCreatedAt(),
            po.getUpdatedAt()
        );
    }
}
