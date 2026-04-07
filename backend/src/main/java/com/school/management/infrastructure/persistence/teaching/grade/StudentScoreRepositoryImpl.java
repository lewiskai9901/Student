package com.school.management.infrastructure.persistence.teaching.grade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.grade.StudentScore;
import com.school.management.domain.teaching.repository.StudentScoreRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class StudentScoreRepositoryImpl implements StudentScoreRepository {
    private final StudentGradeMapper mapper;

    public StudentScoreRepositoryImpl(StudentGradeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public StudentScore save(StudentScore score) {
        StudentGradePO po = toPO(score);
        if (score.getId() == null) {
            mapper.insert(po);
            score.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return score;
    }

    @Override
    public Optional<StudentScore> findById(Long id) {
        StudentGradePO po = mapper.selectById(id);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<StudentScore> findByBatchId(Long batchId) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        w.eq(StudentGradePO::getBatchId, batchId).orderByDesc(StudentGradePO::getCreatedAt);
        return mapper.selectList(w).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<StudentScore> findByStudentId(Long studentId, Long semesterId, Long courseId) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        w.eq(StudentGradePO::getStudentId, studentId);
        if (semesterId != null) w.eq(StudentGradePO::getSemesterId, semesterId);
        if (courseId != null) w.eq(StudentGradePO::getCourseId, courseId);
        w.orderByDesc(StudentGradePO::getCreatedAt);
        return mapper.selectList(w).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<StudentScore> findByClassId(Long orgUnitId) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        w.eq(StudentGradePO::getOrgUnitId, orgUnitId)
         .orderByAsc(StudentGradePO::getCourseId)
         .orderByDesc(StudentGradePO::getTotalScore);
        return mapper.selectList(w).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) { mapper.deleteById(id); }

    private StudentGradePO toPO(StudentScore d) {
        StudentGradePO po = new StudentGradePO();
        po.setId(d.getId()); po.setBatchId(d.getBatchId()); po.setSemesterId(d.getSemesterId());
        po.setTaskId(d.getTaskId()); po.setCourseId(d.getCourseId());
        po.setStudentId(d.getStudentId()); po.setOrgUnitId(d.getOrgUnitId());
        po.setTotalScore(d.getTotalScore()); po.setGradeLevel(d.getGradeLevel());
        po.setGradePoint(d.getGradePoint());
        po.setPassed(d.getPassed() != null && d.getPassed() ? 1 : 0);
        po.setCreditsEarned(d.getCreditsEarned());
        po.setGradeStatus(d.getGradeStatus());
        po.setIsMakeup(d.getIsMakeup() != null && d.getIsMakeup() ? 1 : 0);
        po.setIsRetake(d.getIsRetake() != null && d.getIsRetake() ? 1 : 0);
        po.setRemark(d.getRemark());
        po.setCreatedAt(d.getCreatedAt()); po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private StudentScore toDomain(StudentGradePO po) {
        return StudentScore.reconstruct(po.getId(), po.getBatchId(), po.getSemesterId(),
                po.getTaskId(), po.getCourseId(), po.getStudentId(), po.getOrgUnitId(),
                po.getTotalScore(), po.getGradeLevel(), po.getGradePoint(),
                po.getPassed() != null && po.getPassed() == 1,
                po.getCreditsEarned(), po.getGradeStatus(),
                po.getIsMakeup() != null && po.getIsMakeup() == 1,
                po.getIsRetake() != null && po.getIsRetake() == 1,
                po.getRemark(), po.getCreatedAt(), po.getUpdatedAt());
    }
}
