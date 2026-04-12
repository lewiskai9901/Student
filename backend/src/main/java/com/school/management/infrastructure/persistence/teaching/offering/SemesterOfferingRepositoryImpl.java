package com.school.management.infrastructure.persistence.teaching.offering;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.offering.SemesterOffering;
import com.school.management.domain.teaching.repository.SemesterOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SemesterOfferingRepositoryImpl implements SemesterOfferingRepository {
    private final SemesterOfferingMapper mapper;

    @Override
    public SemesterOffering save(SemesterOffering offering) {
        SemesterOfferingPO po = toPO(offering);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<SemesterOffering> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<SemesterOffering> findBySemesterId(Long semesterId) {
        return mapper.selectList(new LambdaQueryWrapper<SemesterOfferingPO>()
                .eq(SemesterOfferingPO::getSemesterId, semesterId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<SemesterOffering> findBySemesterIdAndGrade(Long semesterId, String grade) {
        return mapper.selectList(new LambdaQueryWrapper<SemesterOfferingPO>()
                .eq(SemesterOfferingPO::getSemesterId, semesterId)
                .eq(SemesterOfferingPO::getApplicableGrade, grade))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private SemesterOfferingPO toPO(SemesterOffering o) {
        SemesterOfferingPO po = new SemesterOfferingPO();
        po.setId(o.getId());
        po.setSemesterId(o.getSemesterId());
        po.setPlanId(o.getPlanId());
        po.setPlanCourseId(o.getPlanCourseId());
        po.setCourseId(o.getCourseId());
        po.setApplicableGrade(o.getApplicableGrade());
        po.setWeeklyHours(o.getWeeklyHours());
        po.setTotalWeeks(o.getTotalWeeks());
        po.setStartWeek(o.getStartWeek());
        po.setEndWeek(o.getEndWeek());
        po.setWeekType(o.getWeekType());
        po.setCourseCategory(o.getCourseCategory());
        po.setCourseType(o.getCourseType());
        po.setAllowCombined(o.getAllowCombined());
        po.setMaxCombinedClasses(o.getMaxCombinedClasses());
        po.setAllowWalking(o.getAllowWalking());
        po.setStatus(o.getStatus());
        po.setRemark(o.getRemark());
        po.setCreatedBy(o.getCreatedBy());
        return po;
    }

    private SemesterOffering toDomain(SemesterOfferingPO po) {
        return SemesterOffering.reconstruct(
                po.getId(), po.getSemesterId(), po.getPlanId(),
                po.getPlanCourseId(), po.getCourseId(),
                po.getApplicableGrade(), po.getWeeklyHours(), po.getTotalWeeks(),
                po.getStartWeek(), po.getEndWeek(), po.getWeekType(), po.getCourseCategory(), po.getCourseType(),
                po.getAllowCombined(), po.getMaxCombinedClasses(), po.getAllowWalking(),
                po.getStatus(), po.getRemark(), po.getCreatedBy()
        );
    }
}
