package com.school.management.infrastructure.persistence.teaching.grade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.grade.GradeBatch;
import com.school.management.domain.teaching.model.grade.GradeType;
import com.school.management.domain.teaching.repository.GradeBatchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class GradeBatchRepositoryImpl implements GradeBatchRepository {
    private final GradeBatchMapper mapper;

    public GradeBatchRepositoryImpl(GradeBatchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public GradeBatch save(GradeBatch batch) {
        GradeBatchPO po = toPO(batch);
        if (batch.getId() == null) {
            mapper.insert(po);
            batch.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return batch;
    }

    @Override
    public Optional<GradeBatch> findById(Long id) {
        GradeBatchPO po = mapper.selectById(id);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<GradeBatch> findBySemester(Long semesterId, Integer gradeType, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<GradeBatchPO> w = new LambdaQueryWrapper<>();
        if (semesterId != null) w.eq(GradeBatchPO::getSemesterId, semesterId);
        if (gradeType != null) w.eq(GradeBatchPO::getGradeType, gradeType);
        if (status != null) w.eq(GradeBatchPO::getStatus, status);
        w.orderByDesc(GradeBatchPO::getCreatedAt);
        return mapper.selectPage(new Page<>(pageNum, pageSize), w).getRecords()
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count(Long semesterId, Integer gradeType, Integer status) {
        LambdaQueryWrapper<GradeBatchPO> w = new LambdaQueryWrapper<>();
        if (semesterId != null) w.eq(GradeBatchPO::getSemesterId, semesterId);
        if (gradeType != null) w.eq(GradeBatchPO::getGradeType, gradeType);
        if (status != null) w.eq(GradeBatchPO::getStatus, status);
        return mapper.selectCount(w);
    }

    @Override
    public void deleteById(Long id) { mapper.deleteById(id); }

    private GradeBatchPO toPO(GradeBatch d) {
        GradeBatchPO po = new GradeBatchPO();
        po.setId(d.getId()); po.setBatchCode(d.getBatchCode()); po.setBatchName(d.getBatchName());
        po.setSemesterId(d.getSemesterId());
        po.setGradeType(d.getGradeType() != null ? d.getGradeType().getCode() : null);
        po.setStartTime(d.getStartTime()); po.setEndTime(d.getEndTime());
        po.setStatus(d.getStatus()); po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt()); po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private GradeBatch toDomain(GradeBatchPO po) {
        return GradeBatch.reconstruct(po.getId(), po.getBatchCode(), po.getBatchName(),
                po.getSemesterId(), GradeType.fromCode(po.getGradeType()),
                po.getStartTime(), po.getEndTime(), po.getStatus(),
                po.getCreatedBy(), po.getCreatedAt(), po.getUpdatedAt());
    }
}
