package com.school.management.infrastructure.persistence.place;

import com.school.management.domain.place.model.entity.PlaceClassAssignment;
import com.school.management.domain.place.repository.PlaceClassAssignmentRepository;
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
public class PlaceClassAssignmentRepositoryImpl implements PlaceClassAssignmentRepository {

    private final PlaceClassAssignmentMapper mapper;

    @Override
    public PlaceClassAssignment save(PlaceClassAssignment assignment) {
        PlaceClassAssignmentPO po = toPO(assignment);
        if (assignment.getId() == null) {
            mapper.insert(po);
            assignment.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return assignment;
    }

    @Override
    public Optional<PlaceClassAssignment> findById(Long id) {
        PlaceClassAssignmentPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<PlaceClassAssignment> findByPlaceId(Long placeId) {
        return mapper.selectByPlaceIdWithRelations(placeId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<PlaceClassAssignment> findByClassId(Long classId) {
        return mapper.selectByClassIdWithRelations(classId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<PlaceClassAssignment> findByOrgUnitId(Long orgUnitId) {
        return mapper.selectByOrgUnitIdWithRelations(orgUnitId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<PlaceClassAssignment> findByPlaceIdAndClassId(Long placeId, Long classId) {
        PlaceClassAssignmentPO po = mapper.selectByPlaceIdAndClassId(placeId, classId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByPlaceId(Long placeId) {
        mapper.deleteByPlaceId(placeId);
    }

    @Override
    public void deleteByClassId(Long classId) {
        mapper.deleteByClassId(classId);
    }

    @Override
    public boolean existsByPlaceIdAndClassId(Long placeId, Long classId) {
        return mapper.checkExists(placeId, classId) > 0;
    }

    @Override
    public int countByPlaceId(Long placeId) {
        return mapper.countByPlaceId(placeId);
    }

    @Override
    public int countByClassId(Long classId) {
        return mapper.countByClassId(classId);
    }

    @Override
    public void batchSave(List<PlaceClassAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        List<PlaceClassAssignmentPO> poList = assignments.stream()
            .map(this::toPO)
            .collect(Collectors.toList());
        mapper.batchInsert(poList);
        // 回填ID
        for (int i = 0; i < assignments.size(); i++) {
            assignments.get(i).setId(poList.get(i).getId());
        }
    }

    // ========== 转换方法 ==========

    private PlaceClassAssignmentPO toPO(PlaceClassAssignment assignment) {
        PlaceClassAssignmentPO po = new PlaceClassAssignmentPO();
        po.setId(assignment.getId());
        po.setPlaceId(assignment.getPlaceId());
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

    private PlaceClassAssignment toDomain(PlaceClassAssignmentPO po) {
        return PlaceClassAssignment.reconstitute(
            po.getId(),
            po.getPlaceId(),
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
