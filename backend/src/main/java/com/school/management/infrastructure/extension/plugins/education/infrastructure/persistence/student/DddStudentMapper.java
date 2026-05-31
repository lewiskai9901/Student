package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.student;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生 Mapper 接口 (DDD架构)
 * 通过JOIN users表获取学生基本信息。
 *
 * <p><b>班级归属来源 (2026-05-31)</b>: 学生的"属于哪个班"不再读
 * {@code user_student.org_unit_id} 行业列, 改走统一 {@code access_relations} 的
 * member 关系 —— subject 是学生的 {@code user_id} (指向 users.id), resource 是班级
 * (org_unit)。因此:
 * <ul>
 *   <li>列表/详情仍返回 {@code org_unit_id} 字段以保持前端契约, 但其值来自
 *       JOIN {@code access_relations ar ON ar.subject_id = s.user_id}
 *       的 {@code ar.resource_id} (而非 s 自己的列)。</li>
 *   <li>按班查 / 统计改用 {@code ar.resource_id = ?}。</li>
 * </ul>
 * member JOIN 条件统一: relation='member', resource_type='org_unit',
 * subject_type='user', ar.deleted=0, 且 (valid_to IS NULL OR valid_to > NOW())。
 *
 * <p>数据权限: {@code viaMembership=true} + {@code membershipSubjectColumn="user_id"}
 * —— 注入 {@code s.user_id IN (SELECT ar.subject_id ... resource_id IN <scope>)},
 * 因为主表 user_student 的行是学生档案, 真正的 subject 是 s.user_id 不是 s.id。
 */
@Mapper
@DataPermission(
    module = "student",
    tableAlias = "s",
    viaMembership = true,
    membershipSubjectColumn = "user_id",
    creatorField = "created_by"
)
public interface DddStudentMapper extends BaseMapper<StudentPO> {

    /** member 归属 JOIN 片段 (s.user_id -> 班级 org_unit), 取 ar.resource_id 作班级 id。 */
    String MEMBER_JOIN = """
        LEFT JOIN access_relations ar
               ON ar.subject_id = s.user_id
              AND ar.relation = 'member'
              AND ar.resource_type = 'org_unit'
              AND ar.subject_type = 'user'
              AND ar.deleted = 0
              AND (ar.valid_to IS NULL OR ar.valid_to > NOW())
        """;

    /**
     * 基础投影: 物理列 {@code s.org_unit_id} 不再作为班级归属来源 —— 显式列出 s 的其余列,
     * 班级 id 改用 {@code ar.resource_id AS org_unit_id} 派生 (前端契约字段名 org_unit_id 不变)。
     *
     * <p>StudentPO 只映射其中一部分列 (id/student_no/user_id/admission_date/graduation_date/
     * student_status/major_id/home_address/emergency_contact/emergency_phone/special_notes/
     * tenant_id/deleted/created_at/updated_at + org_unit_id 派生 + users 派生字段), 多余列
     * MyBatis 自动忽略。这里仍 SELECT 全列是为保持与旧 {@code s.*} 行为最小差异,
     * 仅把 org_unit_id 一列从物理列替换为 member 派生列。
     */
    String BASE_JOIN_SELECT = """
        SELECT s.id, s.student_no, s.id_card_type, s.ethnicity, s.political_status,
               s.user_id, s.grade_id, s.major_id, s.major_direction_id, s.education_level,
               s.study_length, s.degree_type, s.admission_date, s.graduation_date,
               s.student_status, s.guardian_name, s.guardian_phone, s.guardian_relation,
               s.father_name, s.father_id_card, s.father_phone, s.mother_name,
               s.mother_id_card, s.mother_phone, s.guardian_id_card, s.emergency_contact,
               s.emergency_phone, s.emergency_contact_relation, s.home_address,
               s.hukou_province, s.hukou_city, s.hukou_district, s.hukou_address,
               s.hukou_type, s.postal_code, s.is_poverty_registered, s.financial_aid_type,
               s.dormitory_id, s.bed_number, s.health_status, s.allergies, s.special_notes,
               s.created_at, s.updated_at, s.created_by, s.updated_by, s.deleted, s.tenant_id,
               ar.resource_id AS org_unit_id,
               u.real_name as name, u.gender, u.phone, u.identity_card as idCard
        FROM user_student s
        LEFT JOIN users u ON s.user_id = u.id
        """ + MEMBER_JOIN;

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
     * 根据班级ID查询 — 按 member 关系 resource_id 反查该班学生
     */
    @Select(BASE_JOIN_SELECT + " WHERE ar.resource_id = #{orgUnitId} AND s.deleted = 0")
    List<StudentPO> selectByClassId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据班级ID和状态查询 — 按 member 关系 resource_id 反查该班学生
     */
    @Select(BASE_JOIN_SELECT + " WHERE ar.resource_id = #{orgUnitId} AND s.student_status = #{status} AND s.deleted = 0")
    List<StudentPO> selectByClassIdAndStatus(@Param("orgUnitId") Long orgUnitId, @Param("status") Integer status);

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
    @Select("SELECT COUNT(*) FROM user_student WHERE student_no = #{studentNo} AND deleted = 0")
    long countByStudentNo(@Param("studentNo") String studentNo);

    /**
     * 检查身份证号是否存在
     */
    @Select("SELECT COUNT(*) FROM user_student s LEFT JOIN users u ON s.user_id = u.id WHERE u.identity_card = #{idCard} AND s.deleted = 0 AND u.deleted = 0")
    long countByIdCard(@Param("idCard") String idCard);

    /**
     * 统计班级学生数量 — 按 member 关系 (ar.resource_id=班级) 反查归属该班的学生数
     */
    @Select("SELECT COUNT(*) FROM user_student s " +
            "JOIN access_relations ar ON ar.subject_id = s.user_id " +
            "  AND ar.relation = 'member' AND ar.resource_type = 'org_unit' " +
            "  AND ar.subject_type = 'user' AND ar.deleted = 0 " +
            "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
            "WHERE ar.resource_id = #{orgUnitId} AND s.deleted = 0")
    long countByClassId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 统计班级在读学生数量 (student_status=1 即 StudentStatus.STUDYING) — 按 member 关系反查
     */
    @Select("SELECT COUNT(*) FROM user_student s " +
            "JOIN access_relations ar ON ar.subject_id = s.user_id " +
            "  AND ar.relation = 'member' AND ar.resource_type = 'org_unit' " +
            "  AND ar.subject_type = 'user' AND ar.deleted = 0 " +
            "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
            "WHERE ar.resource_id = #{orgUnitId} AND s.student_status = 1 AND s.deleted = 0")
    long countActiveByClassId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 统计所有学生数量
     */
    @Select("SELECT COUNT(*) FROM user_student WHERE deleted = 0")
    long countAll();

    /**
     * 按关键字统计学生数量
     */
    @Select("""
        SELECT COUNT(*) FROM user_student s
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
    @Select("SELECT COUNT(*) FROM user_student s " +
            "LEFT JOIN users u ON s.user_id = u.id " +
            "JOIN access_relations ar ON ar.subject_id = s.user_id " +
            "  AND ar.relation = 'member' AND ar.resource_type = 'org_unit' " +
            "  AND ar.subject_type = 'user' AND ar.deleted = 0 " +
            "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
            "WHERE ar.resource_id = #{orgUnitId} AND u.gender = #{gender} AND s.deleted = 0")
    long countByClassIdAndGender(@Param("orgUnitId") Long orgUnitId, @Param("gender") Integer gender);
}
