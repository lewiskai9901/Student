package com.school.management.infrastructure.persistence.teaching;

import com.school.management.domain.teaching.model.CohortSemesterMapping;
import com.school.management.domain.teaching.repository.CohortSemesterMappingRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CohortSemesterMappingRepositoryImpl implements CohortSemesterMappingRepository {

    private final CohortSemesterMappingMapper mapper;

    public CohortSemesterMappingRepositoryImpl(CohortSemesterMappingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CohortSemesterMapping save(CohortSemesterMapping m) {
        CohortSemesterMappingPO po = toPO(m);
        if (m.getId() == null) {
            mapper.insert(po);
            m.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return m;
    }

    @Override
    public Optional<CohortSemesterMapping> findByCohortAndSemester(Long cohortId, Long semesterId) {
        CohortSemesterMappingPO po = mapper.findByCohortAndSemester(cohortId, semesterId);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<CohortSemesterMapping> findBySemesterId(Long semesterId) {
        return mapper.findBySemesterId(semesterId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CohortSemesterMapping> findByCohortId(Long cohortId) {
        return mapper.findByCohortId(cohortId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private CohortSemesterMappingPO toPO(CohortSemesterMapping d) {
        CohortSemesterMappingPO po = new CohortSemesterMappingPO();
        po.setId(d.getId());
        po.setCohortId(d.getCohortId());
        po.setSemesterId(d.getSemesterId());
        po.setProgramSemester(d.getProgramSemester());
        po.setPlanId(d.getPlanId());
        po.setStatus(d.getStatus());
        return po;
    }

    private CohortSemesterMapping toDomain(CohortSemesterMappingPO po) {
        return CohortSemesterMapping.reconstruct(po.getId(), po.getCohortId(), po.getSemesterId(),
                po.getProgramSemester(), po.getPlanId(), po.getStatus());
    }
}
