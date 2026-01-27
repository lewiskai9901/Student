package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.aggregate.StudentGrade;
import com.school.management.domain.teaching.model.entity.GradeItem;
import com.school.management.domain.teaching.repository.StudentGradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StudentGradeRepositoryImpl implements StudentGradeRepository {

    private final StudentGradeMapper gradeMapper;
    private final GradeItemMapper itemMapper;

    @Override
    public StudentGrade save(StudentGrade grade) {
        StudentGradePO po = toPO(grade);
        if (po.getId() == null) gradeMapper.insert(po);
        else gradeMapper.updateById(po);
        grade.setId(po.getId());
        return grade;
    }

    @Override
    public Optional<StudentGrade> findById(Long id) {
        return Optional.ofNullable(gradeMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<StudentGrade> findByBatchIdAndStudentId(Long batchId, Long studentId) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        w.eq(StudentGradePO::getBatchId, batchId).eq(StudentGradePO::getStudentId, studentId);
        return Optional.ofNullable(gradeMapper.selectOne(w)).map(this::toDomain);
    }

    @Override
    public List<StudentGrade> findByBatchId(Long batchId) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        w.eq(StudentGradePO::getBatchId, batchId);
        return gradeMapper.selectList(w).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<StudentGrade> findBySemesterId(Long semesterId) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        w.eq(StudentGradePO::getSemesterId, semesterId);
        return gradeMapper.selectList(w).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<StudentGrade> findByStudentIdAndSemesterId(Long studentId, Long semesterId) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        w.eq(StudentGradePO::getStudentId, studentId).eq(StudentGradePO::getSemesterId, semesterId);
        return gradeMapper.selectList(w).stream().map(po -> {
            StudentGrade g = toDomain(po);
            g.setItems(findGradeItems(g.getId()));
            return g;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveGradeItems(Long gradeId, List<GradeItem> items) {
        itemMapper.deleteByGradeId(gradeId);
        for (GradeItem item : items) {
            GradeItemPO po = toItemPO(item);
            po.setGradeId(gradeId);
            po.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(po);
            item.setId(po.getId());
        }
    }

    @Override
    public List<GradeItem> findGradeItems(Long gradeId) {
        LambdaQueryWrapper<GradeItemPO> w = new LambdaQueryWrapper<>();
        w.eq(GradeItemPO::getGradeId, gradeId);
        return itemMapper.selectList(w).stream().map(this::toItemDomain).collect(Collectors.toList());
    }

    @Override
    public List<StudentGrade> findBatchesPage(int page, int size, Long semesterId, Long courseId, Integer status) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        if (semesterId != null) w.eq(StudentGradePO::getSemesterId, semesterId);
        if (courseId != null) w.eq(StudentGradePO::getCourseId, courseId);
        if (status != null) w.eq(StudentGradePO::getStatus, status);
        w.groupBy(StudentGradePO::getBatchId).orderByDesc(StudentGradePO::getCreatedAt);
        return gradeMapper.selectPage(new Page<>(page, size), w).getRecords().stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countBatches(Long semesterId, Long courseId, Integer status) {
        LambdaQueryWrapper<StudentGradePO> w = new LambdaQueryWrapper<>();
        if (semesterId != null) w.eq(StudentGradePO::getSemesterId, semesterId);
        if (courseId != null) w.eq(StudentGradePO::getCourseId, courseId);
        if (status != null) w.eq(StudentGradePO::getStatus, status);
        return gradeMapper.selectCount(w);
    }

    private StudentGradePO toPO(StudentGrade d) {
        StudentGradePO po = new StudentGradePO();
        po.setId(d.getId()); po.setBatchId(d.getBatchId()); po.setStudentId(d.getStudentId());
        po.setSemesterId(d.getSemesterId()); po.setCourseId(d.getCourseId()); po.setClassId(d.getClassId());
        po.setBatchName(d.getBatchName()); po.setGradeType(d.getGradeType()); po.setTotalScore(d.getTotalScore());
        po.setGradeLevel(d.getGradeLevel()); po.setGradePoint(d.getGradePoint()); po.setStatus(d.getStatus());
        po.setRemark(d.getRemark()); po.setCreatedBy(d.getCreatedBy()); po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedBy(d.getUpdatedBy()); po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private StudentGrade toDomain(StudentGradePO po) {
        return StudentGrade.builder().id(po.getId()).batchId(po.getBatchId()).studentId(po.getStudentId())
                .semesterId(po.getSemesterId()).courseId(po.getCourseId()).classId(po.getClassId())
                .batchName(po.getBatchName()).gradeType(po.getGradeType()).totalScore(po.getTotalScore())
                .gradeLevel(po.getGradeLevel()).gradePoint(po.getGradePoint()).status(po.getStatus())
                .remark(po.getRemark()).createdBy(po.getCreatedBy()).createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy()).updatedAt(po.getUpdatedAt()).build();
    }

    private GradeItemPO toItemPO(GradeItem d) {
        GradeItemPO po = new GradeItemPO();
        po.setId(d.getId()); po.setGradeId(d.getGradeId()); po.setItemName(d.getItemName());
        po.setScore(d.getScore()); po.setWeight(d.getWeight());
        return po;
    }

    private GradeItem toItemDomain(GradeItemPO po) {
        return GradeItem.builder().id(po.getId()).gradeId(po.getGradeId()).itemName(po.getItemName())
                .score(po.getScore()).weight(po.getWeight()).build();
    }
}
