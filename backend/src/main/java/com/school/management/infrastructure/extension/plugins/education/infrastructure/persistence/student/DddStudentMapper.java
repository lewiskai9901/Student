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
// 不配 creatorField: user_student 无 created_by 列 (V20260531 已删), viaMembership 的
// SELF 语义走 membershipSubjectColumn (s.user_id = 本人), 拦截器各路径均不发出 creatorField。
@DataPermission(
    module = "student",
    tableAlias = "s",
    viaMembership = true,
    membershipSubjectColumn = "user_id"
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
     * 基础投影: 只投 user_student 真实存在的物理列 (2026-06-09 修复)。旧版本投影了
     * 32 个早已从表上 DROP 的行业列 (id_card_type / ethnicity / grade_id / major_id /
     * guardian_xxx / hukou_xxx / created_by / ... 见 V20260531 迁移), 导致
     * "Unknown column s.id_card_type" 错误把整条学生读路径打成 500 (code 2005),
     * 所有用户(含超管)受影响。
     *
     * <p><b>展示字段来源 = 内联 s.* 列</b>: user_student 表自身已有 inline
     * {@code name/gender/id_card/phone/email/birth_date/avatar_url} 列, 且实测数据填充正确
     * ({@code s.gender}=1/2 正确, 而 {@code users.gender}=0 是错的)。故不再 JOIN users,
     * 直接取 s 列, 少一个 JOIN 且取表自身权威数据。
     *
     * <p>StudentPO 映射的列 (含 @TableField(exist=false) 别名): id, student_no, user_id,
     * name, gender, id_card as idCard, phone, email, birth_date as birthDate,
     * admission_date as enrollmentDate, graduation_date as expectedGraduationDate,
     * student_status as status, avatar_url as avatarUrl, home_address, emergency_contact,
     * emergency_phone, special_notes as remark, tenant_id, deleted, created_at, updated_at,
     * 外加派生 {@code ar.resource_id AS org_unit_id} (member 关系班级归属)。
     */
    String BASE_JOIN_SELECT = """
        SELECT s.id, s.student_no, s.user_id,
               s.name, s.gender, s.id_card AS idCard, s.phone, s.email,
               s.birth_date, s.avatar_url,
               s.enrollment_date, s.expected_graduation_date,
               s.admission_date, s.graduation_date, s.student_status,
               s.dormitory_id, s.bed_number,
               s.home_address, s.emergency_contact, s.emergency_phone,
               s.remark, s.special_notes,
               s.created_at, s.updated_at, s.deleted, s.tenant_id,
               ar.resource_id AS org_unit_id
        FROM user_student s
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
    @Select(BASE_JOIN_SELECT + " WHERE s.id_card = #{idCard} AND s.deleted = 0")
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
             OR s.name LIKE CONCAT('%', #{keyword}, '%')
             OR s.phone LIKE CONCAT('%', #{keyword}, '%'))
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
    @Select("SELECT COUNT(*) FROM user_student WHERE id_card = #{idCard} AND deleted = 0")
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
        WHERE s.deleted = 0
        AND (s.student_no LIKE CONCAT('%', #{keyword}, '%')
             OR s.name LIKE CONCAT('%', #{keyword}, '%')
             OR s.phone LIKE CONCAT('%', #{keyword}, '%'))
        """)
    long countByKeyword(@Param("keyword") String keyword);

    /**
     * 统计班级中指定性别的学生数量
     * @param classId 班级ID
     * @param gender 性别代码 (1=男, 2=女)
     */
    @Select("SELECT COUNT(*) FROM user_student s " +
            "JOIN access_relations ar ON ar.subject_id = s.user_id " +
            "  AND ar.relation = 'member' AND ar.resource_type = 'org_unit' " +
            "  AND ar.subject_type = 'user' AND ar.deleted = 0 " +
            "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
            "WHERE ar.resource_id = #{orgUnitId} AND s.gender = #{gender} AND s.deleted = 0")
    long countByClassIdAndGender(@Param("orgUnitId") Long orgUnitId, @Param("gender") Integer gender);
}
