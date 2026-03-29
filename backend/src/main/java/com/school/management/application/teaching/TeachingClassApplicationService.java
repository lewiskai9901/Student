package com.school.management.application.teaching;

import com.school.management.domain.teaching.model.teachingclass.TeachingClass;
import com.school.management.domain.teaching.model.teachingclass.TeachingClassMember;
import com.school.management.domain.teaching.model.teachingclass.TeachingClassType;
import com.school.management.domain.teaching.repository.TeachingClassRepository;
import com.school.management.domain.teaching.repository.TeachingClassMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class TeachingClassApplicationService {
    private final TeachingClassRepository classRepo;
    private final TeachingClassMemberRepository memberRepo;

    public List<TeachingClass> listBySemester(Long semesterId) {
        List<TeachingClass> classes = classRepo.findBySemesterId(semesterId);
        for (TeachingClass tc : classes) {
            tc.setMembers(memberRepo.findByTeachingClassId(tc.getId()));
        }
        return classes;
    }

    public TeachingClass getById(Long id) {
        TeachingClass tc = classRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("教学班不存在: " + id));
        tc.setMembers(memberRepo.findByTeachingClassId(tc.getId()));
        return tc;
    }

    public TeachingClass create(Map<String, Object> data, Long userId) {
        TeachingClass tc = TeachingClass.create(
            toLong(data.get("semesterId")),
            (String) data.get("className"),
            toLong(data.get("courseId")),
            TeachingClassType.fromCode(toInt(data.getOrDefault("classType", 1))),
            toInt(data.get("weeklyHours")),
            userId
        );
        if (data.containsKey("requiredRoomType") || data.containsKey("startWeek")) {
            tc.update(
                (String) data.get("className"),
                toInt(data.get("weeklyHours")),
                (String) data.get("requiredRoomType"),
                toInt(data.get("requiredCapacity")),
                toInt(data.getOrDefault("startWeek", 1)),
                toInt(data.get("endWeek")),
                (String) data.get("remark")
            );
        }
        return classRepo.save(tc);
    }

    public TeachingClass update(Long id, Map<String, Object> data) {
        TeachingClass tc = classRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("教学班不存在: " + id));
        tc.update(
            (String) data.get("className"),
            toInt(data.get("weeklyHours")),
            (String) data.get("requiredRoomType"),
            toInt(data.get("requiredCapacity")),
            toInt(data.get("startWeek")),
            toInt(data.get("endWeek")),
            (String) data.get("remark")
        );
        return classRepo.save(tc);
    }

    public void delete(Long id) {
        memberRepo.deleteByTeachingClassId(id);
        classRepo.deleteById(id);
    }

    public List<TeachingClassMember> getMembers(Long teachingClassId) {
        return memberRepo.findByTeachingClassId(teachingClassId);
    }

    public void addMembers(Long teachingClassId, List<Map<String, Object>> membersData) {
        List<TeachingClassMember> members = new ArrayList<>();
        for (Map<String, Object> m : membersData) {
            int memberType = toInt(m.get("memberType"));
            if (memberType == 1) {
                members.add(TeachingClassMember.ofAdminClass(teachingClassId, toLong(m.get("adminClassId"))));
            } else {
                members.add(TeachingClassMember.ofStudent(teachingClassId, toLong(m.get("studentId"))));
            }
        }
        memberRepo.saveAll(members);
    }

    public void removeMembers(Long teachingClassId) {
        memberRepo.deleteByTeachingClassId(teachingClassId);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }
}
