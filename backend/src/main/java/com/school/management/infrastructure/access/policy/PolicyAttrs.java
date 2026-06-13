package com.school.management.infrastructure.access.policy;

/**
 * {@link AccessRequest#attrs} 的标准键名。
 */
public final class PolicyAttrs {
    private PolicyAttrs() {}

    /** Boolean：目标用户是否为超管 */
    public static final String TARGET_IS_SUPER_ADMIN = "target.isSuperAdmin";

    /** Set&lt;Long&gt;：目标用户持 admin 关系的组织 id 集（"他管理哪些组织"） */
    public static final String TARGET_MANAGED_ORG_IDS = "target.managedOrgIds";

    /** Boolean：本次授/改的角色里是否含超管角色（提权信号） */
    public static final String ASSIGNS_SUPER_ADMIN = "assignsSuperAdmin";
}
