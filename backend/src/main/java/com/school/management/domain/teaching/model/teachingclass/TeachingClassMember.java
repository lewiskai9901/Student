package com.school.management.domain.teaching.model.teachingclass;

import lombok.Getter;

@Getter
public class TeachingClassMember {
    private Long id;
    private Long teachingClassId;
    private Integer memberType; // 1=admin_class, 2=student
    private Long adminClassId;
    private Long studentId;

    protected TeachingClassMember() {}

    public static TeachingClassMember ofAdminClass(Long teachingClassId, Long adminClassId) {
        TeachingClassMember m = new TeachingClassMember();
        m.teachingClassId = teachingClassId;
        m.memberType = 1;
        m.adminClassId = adminClassId;
        return m;
    }

    public static TeachingClassMember ofStudent(Long teachingClassId, Long studentId) {
        TeachingClassMember m = new TeachingClassMember();
        m.teachingClassId = teachingClassId;
        m.memberType = 2;
        m.studentId = studentId;
        return m;
    }

    public static TeachingClassMember reconstruct(Long id, Long teachingClassId,
            Integer memberType, Long adminClassId, Long studentId) {
        TeachingClassMember m = new TeachingClassMember();
        m.id = id;
        m.teachingClassId = teachingClassId;
        m.memberType = memberType;
        m.adminClassId = adminClassId;
        m.studentId = studentId;
        return m;
    }
}
