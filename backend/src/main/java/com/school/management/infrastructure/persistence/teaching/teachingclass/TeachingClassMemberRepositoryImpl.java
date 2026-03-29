package com.school.management.infrastructure.persistence.teaching.teachingclass;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.teachingclass.TeachingClassMember;
import com.school.management.domain.teaching.repository.TeachingClassMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TeachingClassMemberRepositoryImpl implements TeachingClassMemberRepository {
    private final TeachingClassMemberMapper mapper;

    @Override
    public void saveAll(List<TeachingClassMember> members) {
        for (TeachingClassMember member : members) {
            TeachingClassMemberPO po = toPO(member);
            if (po.getId() == null) {
                mapper.insert(po);
            } else {
                mapper.updateById(po);
            }
        }
    }

    @Override
    public List<TeachingClassMember> findByTeachingClassId(Long teachingClassId) {
        return mapper.selectList(new LambdaQueryWrapper<TeachingClassMemberPO>()
                .eq(TeachingClassMemberPO::getTeachingClassId, teachingClassId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByTeachingClassId(Long teachingClassId) {
        mapper.delete(new LambdaQueryWrapper<TeachingClassMemberPO>()
                .eq(TeachingClassMemberPO::getTeachingClassId, teachingClassId));
    }

    private TeachingClassMemberPO toPO(TeachingClassMember m) {
        TeachingClassMemberPO po = new TeachingClassMemberPO();
        po.setId(m.getId());
        po.setTeachingClassId(m.getTeachingClassId());
        po.setMemberType(m.getMemberType());
        po.setAdminClassId(m.getAdminClassId());
        po.setStudentId(m.getStudentId());
        return po;
    }

    private TeachingClassMember toDomain(TeachingClassMemberPO po) {
        return TeachingClassMember.reconstruct(
                po.getId(), po.getTeachingClassId(),
                po.getMemberType(), po.getAdminClassId(), po.getStudentId()
        );
    }
}
