package com.school.management.infrastructure.persistence.teaching.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.exam.ExamBatch;
import com.school.management.domain.teaching.model.exam.ExamType;
import com.school.management.domain.teaching.repository.ExamBatchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ExamBatchRepositoryImpl implements ExamBatchRepository {
    private final ExamBatchMapper mapper;

    public ExamBatchRepositoryImpl(ExamBatchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ExamBatch save(ExamBatch batch) {
        ExamBatchPO po = toPO(batch);
        if (batch.getId() == null) {
            mapper.insert(po);
            batch.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return batch;
    }

    @Override
    public Optional<ExamBatch> findById(Long id) {
        ExamBatchPO po = mapper.selectById(id);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<ExamBatch> findBySemester(Long semesterId, Integer examType, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<ExamBatchPO> w = new LambdaQueryWrapper<>();
        if (semesterId != null) w.eq(ExamBatchPO::getSemesterId, semesterId);
        if (examType != null) w.eq(ExamBatchPO::getExamType, examType);
        if (status != null) w.eq(ExamBatchPO::getStatus, status);
        w.orderByDesc(ExamBatchPO::getCreatedAt);
        return mapper.selectPage(new Page<>(pageNum, pageSize), w).getRecords()
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count(Long semesterId, Integer examType, Integer status) {
        LambdaQueryWrapper<ExamBatchPO> w = new LambdaQueryWrapper<>();
        if (semesterId != null) w.eq(ExamBatchPO::getSemesterId, semesterId);
        if (examType != null) w.eq(ExamBatchPO::getExamType, examType);
        if (status != null) w.eq(ExamBatchPO::getStatus, status);
        return mapper.selectCount(w);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ExamBatchPO toPO(ExamBatch d) {
        ExamBatchPO po = new ExamBatchPO();
        po.setId(d.getId());
        po.setBatchCode(d.getBatchCode());
        po.setBatchName(d.getBatchName());
        po.setSemesterId(d.getSemesterId());
        po.setExamType(d.getExamType() != null ? d.getExamType().getCode() : null);
        po.setStartDate(d.getStartDate());
        po.setEndDate(d.getEndDate());
        po.setStatus(d.getStatus());
        po.setRemark(d.getRemark());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ExamBatch toDomain(ExamBatchPO po) {
        return ExamBatch.reconstruct(po.getId(), po.getBatchCode(), po.getBatchName(),
                po.getSemesterId(), ExamType.fromCode(po.getExamType()),
                po.getStartDate(), po.getEndDate(), po.getStatus(), po.getRemark(),
                po.getCreatedBy(), po.getCreatedAt(), po.getUpdatedAt());
    }
}
