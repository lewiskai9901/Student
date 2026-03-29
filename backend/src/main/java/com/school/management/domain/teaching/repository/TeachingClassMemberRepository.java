package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.teachingclass.TeachingClassMember;
import java.util.List;

public interface TeachingClassMemberRepository {
    void saveAll(List<TeachingClassMember> members);
    List<TeachingClassMember> findByTeachingClassId(Long teachingClassId);
    void deleteByTeachingClassId(Long teachingClassId);
}
