package com.school.management.infrastructure.persistence.organization;

import com.school.management.domain.organization.model.Major;
import com.school.management.domain.organization.model.MajorDirection;
import com.school.management.domain.organization.repository.MajorRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 专业仓储实现
 */
@Repository
public class MajorRepositoryImpl implements MajorRepository {

    private final MajorMapper majorMapper;
    private final MajorDirectionMapper directionMapper;

    public MajorRepositoryImpl(MajorMapper majorMapper, MajorDirectionMapper directionMapper) {
        this.majorMapper = majorMapper;
        this.directionMapper = directionMapper;
    }

    @Override
    public Optional<Major> findById(Long id) {
        MajorPO po = majorMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Major save(Major major) {
        MajorPO po = toPO(major);

        if (major.getId() == null) {
            majorMapper.insert(po);
            major.setId(po.getId());
        } else {
            majorMapper.updateById(po);
        }

        // 保存专业方向
        saveDirections(major);

        return major;
    }

    @Override
    public void delete(Major aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            majorMapper.deleteById(aggregate.getId());
            // 删除关联的专业方向
            List<MajorDirectionPO> directions = directionMapper.findByMajorId(aggregate.getId());
            for (MajorDirectionPO direction : directions) {
                directionMapper.deleteById(direction.getId());
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        majorMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return majorMapper.selectById(id) != null;
    }

    @Override
    public Optional<Major> findByMajorCode(String majorCode) {
        MajorPO po = majorMapper.findByMajorCode(majorCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Major> findByOrgUnitId(Long orgUnitId) {
        return majorMapper.findByDepartmentId(orgUnitId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Major> findAllEnabled() {
        return majorMapper.findAllEnabled().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByMajorCode(String majorCode) {
        return majorMapper.countByMajorCode(majorCode) > 0;
    }

    @Override
    public int countByOrgUnitId(Long orgUnitId) {
        return majorMapper.countByDepartmentId(orgUnitId);
    }

    @Override
    public Optional<Major> findByDirectionId(Long directionId) {
        MajorDirectionPO directionPO = directionMapper.selectById(directionId);
        if (directionPO == null) {
            return Optional.empty();
        }
        return findById(directionPO.getMajorId());
    }

    @Override
    public Optional<MajorDirection> findDirectionById(Long directionId) {
        MajorDirectionPO po = directionMapper.selectById(directionId);
        return Optional.ofNullable(po).map(this::toDirectionDomain);
    }

    @Override
    public Optional<MajorDirection> findDirectionByCode(String directionCode) {
        MajorDirectionPO po = directionMapper.findByDirectionCode(directionCode);
        return Optional.ofNullable(po).map(this::toDirectionDomain);
    }

    @Override
    public List<MajorDirection> findDirectionsByMajorId(Long majorId) {
        return directionMapper.findByMajorId(majorId).stream()
            .map(this::toDirectionDomain)
            .collect(Collectors.toList());
    }

    private void saveDirections(Major major) {
        if (major.getId() == null) return;

        // 获取现有的专业方向
        List<MajorDirectionPO> existingDirections = directionMapper.findByMajorId(major.getId());

        // 保存/更新专业方向
        for (MajorDirection direction : major.getDirections()) {
            MajorDirectionPO po = toDirectionPO(direction, major.getId());
            if (direction.getId() == null) {
                directionMapper.insert(po);
                direction.setId(po.getId());
            } else {
                directionMapper.updateById(po);
            }
        }

        // 删除不再存在的专业方向
        List<Long> currentIds = major.getDirections().stream()
            .map(MajorDirection::getId)
            .filter(id -> id != null)
            .collect(Collectors.toList());

        for (MajorDirectionPO existing : existingDirections) {
            if (!currentIds.contains(existing.getId())) {
                directionMapper.deleteById(existing.getId());
            }
        }
    }

    private MajorPO toPO(Major domain) {
        MajorPO po = new MajorPO();
        po.setId(domain.getId());
        po.setMajorCode(domain.getMajorCode());
        po.setMajorName(domain.getMajorName());
        po.setDepartmentId(domain.getOrgUnitId());
        po.setDescription(domain.getDescription());
        po.setSortOrder(domain.getSortOrder());
        po.setStatus(domain.isEnabled() ? 1 : 0);
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedBy(domain.getUpdatedBy());
        return po;
    }

    private Major toDomain(MajorPO po) {
        // 加载专业方向
        List<MajorDirection> directions = directionMapper.findByMajorId(po.getId()).stream()
            .map(this::toDirectionDomain)
            .collect(Collectors.toList());

        return Major.builder()
            .id(po.getId())
            .majorCode(po.getMajorCode())
            .majorName(po.getMajorName())
            .orgUnitId(po.getDepartmentId())
            .description(po.getDescription())
            .directions(directions)
            .enabled(po.getStatus() != null && po.getStatus() == 1)
            .sortOrder(po.getSortOrder() != null ? po.getSortOrder() : 0)
            .createdBy(po.getCreatedBy())
            .build();
    }

    private MajorDirectionPO toDirectionPO(MajorDirection domain, Long majorId) {
        MajorDirectionPO po = new MajorDirectionPO();
        po.setId(domain.getId());
        po.setMajorId(majorId);
        po.setDirectionCode(domain.getDirectionCode());
        po.setDirectionName(domain.getDirectionName());
        po.setLevel(domain.getLevel());
        po.setYears(domain.getYears());
        po.setIsSegmented(domain.isSegmented() != null && domain.isSegmented() ? 1 : 0);
        po.setPhase1Level(domain.getPhase1Level());
        po.setPhase1Years(domain.getPhase1Years());
        po.setPhase2Level(domain.getPhase2Level());
        po.setPhase2Years(domain.getPhase2Years());
        po.setRemarks(domain.getRemarks());
        po.setStatus(domain.isEnabled() != null && domain.isEnabled() ? 1 : 0);
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private MajorDirection toDirectionDomain(MajorDirectionPO po) {
        return MajorDirection.reconstitute(
            po.getId(),
            po.getDirectionCode(),
            po.getDirectionName(),
            po.getLevel(),
            po.getYears(),
            po.getIsSegmented() != null && po.getIsSegmented() == 1,
            po.getPhase1Level(),
            po.getPhase1Years(),
            po.getPhase2Level(),
            po.getPhase2Years(),
            po.getRemarks(),
            po.getStatus() != null && po.getStatus() == 1,
            po.getCreatedAt(),
            po.getUpdatedAt()
        );
    }
}
