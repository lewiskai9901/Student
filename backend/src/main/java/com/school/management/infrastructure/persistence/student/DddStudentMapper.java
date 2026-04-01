package com.school.management.infrastructure.persistence.student;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生 Mapper 接口 (DDD架构)
 * 通过JOIN users表获取学生基本信息
 */
@Mapper
@DataPermission(
    module = "student",
    resourceType = "student",
    tableAlias = "s",
    orgUnitField = "org_unit_id",
    creatorField = "created_by"
)
public interface DddStudentMapper extends BaseMapper<StudentPO> {

    String BASE_JOIN_SELECT = """
        SELECT s.*, u.real_name as name, u.gender, u.phone, u.identity_card as idCard
        FROM students s
        LEFT JOIN users u ON s.user_id = u.id
        """;

    /**
     * 根据ID查询(带用户信息)
     */
    @Select(BASE_JOIN_SELECT + " WHERE s.id = #{id} AND s.deleted = 0")
    StudentPO selectByIdWithUser(@Param("id") Long id);

    /**
     * 根据学号查询
     */
    @Select(BASE_JOIN_SELECT + " WHERE s.student_no = #{studentNo} AND s.deleted = 0")
    StudentPO selectByStudentNo(@Param("studentNo") String studentNo);

    /**
     * 根据身份证号查询
     */
    @Select(BASE_JOIN_SELECT + " WHERE u.identity_card = #{idCard} AND s.deleted = 0 AND u.deleted = 0")
    StudentPO selectByIdCard(@Param("idCard") String idCard);

    /**
     * 根据班级ID查询
     */
    @Select(BASE_JOIN_SELECT + " WHERE s.class_id = #{classId} AND s.deleted = 0")
    List<StudentPO> selectByClassId(@Param("classId") Long classId);

    /**
     * 根据班级ID和状态查询
     */
    @Select(BASE_JOIN_SELECT + " WHERE s.class_id = #{classId} AND s.student_status = #{status} AND s.deleted = 0")
    List<StudentPO> selectByClassIdAndStatus(@Param("classId") Long classId, @Param("status") Integer status);

    /**
     * 分页查询学生(带用户信息)
     */
    @Select(BASE_JOIN_SELECT + " WHERE s.deleted = 0 ORDER BY s.created_at DESC LIMIT #{offset}, #{limit}")
    List<StudentPO> selectPageWithUser(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按关键字分页查询学生
     */
    @Select(BASE_JOIN_SELECT + """
        WHERE s.deleted = 0
        AND (s.student_no LIKE CONCAT('%', #{keyword}, '%')
             OR u.real_name LIKE CONCAT('%', #{keyword}, '%')
             OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
        ORDER BY s.created_at DESC
        LIMIT #{offset}, #{limit}
        """)
    List<StudentPO> selectPageByKeyword(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 检查学号是否存在
     */
    @Select("SELECT COUNT(*) FROM students WHERE student_no = #{studentNo} AND deleted = 0")
    long countByStudentNo(@Param("studentNo") String studentNo);

    /**
     * 检查身份证号是否存在
     */
    @Select("SELECT COUNT(*) FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE u.identity_card = #{idCard} AND s.deleted = 0 AND u.deleted = 0")
    long countByIdCard(@Param("idCard") String idCard);

    /**
     * 统计班级学生数量
     */
    @Select("SELECT COUNT(*) FROM students WHERE class_id = #{classId} AND deleted = 0")
    long countByClassId(@Param("classId") Long classId);

    /**
     * 统计班级在读学生数量 (status=0 表示在读)
     */
    @Select("SELECT COUNT(*) FROM students WHERE class_id = #{classId} AND student_status = 0 AND deleted = 0")
    long countActiveByClassId(@Param("classId") Long classId);

    /**
     * 统计所有学生数量
     */
    @Select("SELECT COUNT(*) FROM students WHERE deleted = 0")
    long countAll();

    /**
     * 按关键字统计学生数量
     */
    @Select("""
        SELECT COUNT(*) FROM students s
        LEFT JOIN users u ON s.user_id = u.id
        WHERE s.deleted = 0
        AND (s.student_no LIKE CONCAT('%', #{keyword}, '%')
             OR u.real_name LIKE CONCAT('%', #{keyword}, '%')
             OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
        """)
    long countByKeyword(@Param("keyword") String keyword);

    /**
     * 统计班级中指定性别的学生数量
     * @param classId 班级ID
     * @param gender 性别代码 (1=男, 2=女)
     */
    @Select("SELECT COUNT(*) FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE s.class_id = #{classId} AND u.gender = #{gender} AND s.deleted = 0")
    long countByClassIdAndGender(@Param("classId") Long classId, @Param("gender") Integer gender);
}
