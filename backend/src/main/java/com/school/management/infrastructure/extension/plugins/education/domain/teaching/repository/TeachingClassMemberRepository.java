package com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository;

import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.teachingclass.TeachingClassMember;
import java.util.List;

public interface TeachingClassMemberRepository {
    void saveAll(List<TeachingClassMember> members);
    List<TeachingClassMember> findByTeachingClassId(Long teachingClassId);
    void deleteByTeachingClassId(Long teachingClassId);
}
