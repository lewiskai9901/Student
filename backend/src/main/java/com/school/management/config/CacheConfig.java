package com.school.management.config;

/**
 * Cache configuration constants.
 *
 * <p>Provides cache name constants for different data types:
 * <ul>
 *   <li>Short-lived: Session data, frequently changing data (5 minutes)</li>
 *   <li>Medium-lived: User data, permissions (30 minutes)</li>
 *   <li>Long-lived: Configuration, static data (2 hours)</li>
 * </ul>
 *
 * <p>Note: Redis beans are configured in {@link RedisConfig}.
 */
public final class CacheConfig {

    private CacheConfig() {
        // Utility class
    }

    /**
     * Cache names and their purposes.
     */
    public static final String CACHE_USER = "user";
    public static final String CACHE_PERMISSION = "permission";
    public static final String CACHE_ROLE = "role";
    public static final String CACHE_CONFIG = "config";
    public static final String CACHE_TEMPLATE = "template";
    public static final String CACHE_DEPARTMENT = "department";
    public static final String CACHE_CLASS = "class";
    public static final String CACHE_RATING_CONFIG = "ratingConfig";
    public static final String CACHE_STATISTICS = "statistics";
}
