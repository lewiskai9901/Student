package com.school.management.infrastructure.persistence.teaching.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.exam.ExamArrangement;
import com.school.management.domain.teaching.repository.ExamArrangementRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ExamArrangementRepositoryImpl implements ExamArrangementRepository {
    private final ExamArrangementMapper mapper;

    public ExamArrangementRepositoryImpl(ExamArrangementMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ExamArrangement save(ExamArrangement a) {
        ExamArrangementPO po = toPO(a);
        if (a.getId() == null) {
            mapper.insert(po);
            a.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return a;
    }

    @Override
    public Optional<ExamArrangement> findById(Long id) {
        ExamArrangementPO po = mapper.selectById(id);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<ExamArrangement> findByBatchId(Long batchId) {
        LambdaQueryWrapper<ExamArrangementPO> w = new LambdaQueryWrapper<>();
        w.eq(ExamArrangementPO::getBatchId, batchId)
         .orderByAsc(ExamArrangementPO::getExamDate)
         .orderByAsc(ExamArrangementPO::getStartTime);
        return mapper.selectList(w).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByBatchIdAndId(Long batchId, Long id) {
        LambdaQueryWrapper<ExamArrangementPO> w = new LambdaQueryWrapper<>();
        w.eq(ExamArrangementPO::getBatchId, batchId).eq(ExamArrangementPO::getId, id);
        mapper.delete(w);
    }

    private ExamArrangementPO toPO(ExamArrangement d) {
        ExamArrangementPO po = new ExamArrangementPO();
        po.setId(d.getId());
        po.setBatchId(d.getBatchId());
        po.setCourseId(d.getCourseId());
        po.setExamDate(d.getExamDate());
        po.setStartTime(d.getStartTime());
        po.setEndTime(d.getEndTime());
        po.setDuration(d.getDuration());
        po.setExamForm(d.getExamForm());
        po.setTotalStudents(d.getTotalStudents());
        po.setRemark(d.getRemark());
        po.setStatus(d.getStatus());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private ExamArrangement toDomain(ExamArrangementPO po) {
        return ExamArrangement.reconstruct(po.getId(), po.getBatchId(), po.getCourseId(),
                po.getExamDate(), po.getStartTime(), po.getEndTime(), po.getDuration(),
                po.getExamForm(), po.getTotalStudents(), po.getRemark(), po.getStatus(),
                po.getCreatedBy(), po.getCreatedAt());
    }
}
