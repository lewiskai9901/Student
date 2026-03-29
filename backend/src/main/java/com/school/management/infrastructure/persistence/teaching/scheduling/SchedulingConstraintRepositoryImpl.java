package com.school.management.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.domain.teaching.model.scheduling.ConstraintType;
import com.school.management.domain.teaching.model.scheduling.SchedulingConstraint;
import com.school.management.domain.teaching.repository.SchedulingConstraintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SchedulingConstraintRepositoryImpl implements SchedulingConstraintRepository {
    private final SchedulingConstraintMapper mapper;

    @Override
    public SchedulingConstraint save(SchedulingConstraint constraint) {
        SchedulingConstraintPO po = toPO(constraint);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<SchedulingConstraint> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<SchedulingConstraint> findBySemesterId(Long semesterId) {
        return mapper.selectList(new LambdaQueryWrapper<SchedulingConstraintPO>()
                .eq(SchedulingConstraintPO::getSemesterId, semesterId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<SchedulingConstraint> findBySemesterIdAndLevel(Long semesterId, ConstraintLevel level) {
        return mapper.selectList(new LambdaQueryWrapper<SchedulingConstraintPO>()
                .eq(SchedulingConstraintPO::getSemesterId, semesterId)
                .eq(SchedulingConstraintPO::getConstraintLevel, level.getCode()))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<SchedulingConstraint> findBySemesterIdAndLevelAndTargetId(Long semesterId, ConstraintLevel level, Long targetId) {
        return mapper.selectList(new LambdaQueryWrapper<SchedulingConstraintPO>()
                .eq(SchedulingConstraintPO::getSemesterId, semesterId)
                .eq(SchedulingConstraintPO::getConstraintLevel, level.getCode())
                .eq(SchedulingConstraintPO::getTargetId, targetId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<SchedulingConstraint> findEnabledBySemesterId(Long semesterId) {
        return mapper.selectList(new LambdaQueryWrapper<SchedulingConstraintPO>()
                .eq(SchedulingConstraintPO::getSemesterId, semesterId)
                .eq(SchedulingConstraintPO::getEnabled, true))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private SchedulingConstraintPO toPO(SchedulingConstraint c) {
        SchedulingConstraintPO po = new SchedulingConstraintPO();
        po.setId(c.getId());
        po.setSemesterId(c.getSemesterId());
        po.setConstraintName(c.getConstraintName());
        po.setConstraintLevel(c.getConstraintLevel() != null ? c.getConstraintLevel().getCode() : null);
        po.setTargetId(c.getTargetId());
        po.setTargetName(c.getTargetName());
        po.setConstraintType(c.getConstraintType() != null ? c.getConstraintType().name() : null);
        po.setIsHard(c.getIsHard());
        po.setPriority(c.getPriority());
        po.setParams(c.getParams());
        po.setEffectiveWeeks(c.getEffectiveWeeks());
        po.setEnabled(c.getEnabled());
        po.setCreatedBy(c.getCreatedBy());
        return po;
    }

    private SchedulingConstraint toDomain(SchedulingConstraintPO po) {
        return SchedulingConstraint.reconstruct(
                po.getId(), po.getSemesterId(), po.getConstraintName(),
                po.getConstraintLevel() != null ? ConstraintLevel.fromCode(po.getConstraintLevel()) : null,
                po.getTargetId(), po.getTargetName(),
                po.getConstraintType() != null ? ConstraintType.valueOf(po.getConstraintType()) : null,
                po.getIsHard(), po.getPriority(),
                po.getParams(), po.getEffectiveWeeks(),
                po.getEnabled(), po.getCreatedBy()
        );
    }
}
