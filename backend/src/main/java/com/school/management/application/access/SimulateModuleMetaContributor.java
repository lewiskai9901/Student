package com.school.management.application.access;

import java.util.Map;

/**
 * 数据权限模拟器 — 模块→表元数据贡献点 SPI。
 *
 * <p>模拟器 ({@code DataPermissionSimulateController}) 需要知道每个数据模块对应
 * 哪张表 / 哪个组织过滤列 才能拼 COUNT+样本查询。通用核心只内置通用模块
 * (user/org_unit/role/place); 行业模块 (如教育的 student/school_class) 的表与
 * 归属口径是行业知识, 由行业插件实现本接口登记 —— 与 DashboardSectionContributor
 * 同一套贡献点理念。插件禁用时 bean 不存在, 模拟器对该模块优雅降级
 * ("此模块未实现模拟")。
 *
 * <p>注意贡献的元数据必须与该模块真实的 {@code @DataPermission} 执行口径一致
 * (orgCol 对齐 mapper 的 orgUnitField / membership 模型), 否则模拟结果会与
 * 实际过滤行为漂移, 误导管理员。
 */
public interface SimulateModuleMetaContributor {

    /** 本插件贡献的 moduleCode → 元数据 (含别名码, 如 school_class 与 class 两个键)。 */
    Map<String, SimulateModuleMeta> contribute();

    /**
     * 模块的表元数据。
     *
     * @param table           物理表 (或视图) 名
     * @param nameCol         样本名称列, 可为 null
     * @param orgCol          组织过滤匹配列。非 membership 模块 = 带 org id 的列;
     *                        membership 模块 = 与 member 关系 subject_id 匹配的列
     *                        (如 users.id / user_student.user_id)。可为 null (不支持 org 过滤)
     * @param hasDeleted      是否有逻辑删除列 deleted
     * @param hasCreatedBy    是否有 created_by 列 (SELF scope 模拟依赖)
     * @param membershipBased true = 归属来自 access_relations member 关系
     *                        (orgCol IN member 子查询), 而非物理 org 列
     */
    record SimulateModuleMeta(String table, String nameCol, String orgCol,
                              boolean hasDeleted, boolean hasCreatedBy, boolean membershipBased) {

        public static SimulateModuleMeta orgColumn(String table, String nameCol, String orgCol,
                                                   boolean hasDeleted, boolean hasCreatedBy) {
            return new SimulateModuleMeta(table, nameCol, orgCol, hasDeleted, hasCreatedBy, false);
        }

        public static SimulateModuleMeta membership(String table, String nameCol, String subjectCol,
                                                    boolean hasDeleted, boolean hasCreatedBy) {
            return new SimulateModuleMeta(table, nameCol, subjectCol, hasDeleted, hasCreatedBy, true);
        }
    }
}
