package com.school.management.domain.shared;

/**
 * 领域层共享常量
 * 集中管理业务规则相关的魔法数字和字符串常量
 */
public final class DomainConstants {

    private DomainConstants() {
        // 防止实例化
    }

    // ========== 系统用户 ==========
    /** 系统管理员用户ID */
    public static final Long SYSTEM_ADMIN_ID = 1L;
    /** 系统操作者名称 */
    public static final String SYSTEM_OPERATOR = "system";

    // ========== 字段长度限制 ==========
    /** 编码字段最大长度 */
    public static final int CODE_MAX_LENGTH = 50;
    /** 名称字段最大长度 */
    public static final int NAME_MAX_LENGTH = 100;
    /** 描述字段最大长度 */
    public static final int DESCRIPTION_MAX_LENGTH = 500;
    /** 备注字段最大长度 */
    public static final int REMARK_MAX_LENGTH = 1000;

    // ========== 登录安全 ==========
    /** 最大登录失败次数 */
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    /** 账户锁定时长（分钟） */
    public static final int LOCK_DURATION_MINUTES = 30;

    // ========== 分页参数 ==========
    /** 默认页码 */
    public static final int DEFAULT_PAGE_NUM = 1;
    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;
    /** 最大每页条数 */
    public static final int MAX_PAGE_SIZE = 100;

    // ========== 组织层级 ==========
    /** 根组织ID */
    public static final Long ROOT_ORG_ID = 0L;
    /** 最大组织层级深度 */
    public static final int MAX_ORG_DEPTH = 10;

    // ========== 量化检查 ==========
    /** 满分 */
    public static final int FULL_SCORE = 100;
    /** 基础分 */
    public static final int BASE_SCORE = 100;
    /** 及格分数线 */
    public static final int PASS_SCORE = 60;

    // ========== 时间常量 ==========
    /** 一天的毫秒数 */
    public static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L;
    /** 一小时的毫秒数 */
    public static final long ONE_HOUR_MILLIS = 60 * 60 * 1000L;

    // ========== 缓存过期时间（秒） ==========
    /** 短期缓存过期时间（5分钟） */
    public static final int CACHE_EXPIRE_SHORT = 300;
    /** 中期缓存过期时间（30分钟） */
    public static final int CACHE_EXPIRE_MEDIUM = 1800;
    /** 长期缓存过期时间（24小时） */
    public static final int CACHE_EXPIRE_LONG = 86400;
}
