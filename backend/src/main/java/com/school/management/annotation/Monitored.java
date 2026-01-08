package com.school.management.annotation;

import java.lang.annotation.*;

/**
 * Marks a method for performance monitoring.
 *
 * <p>Methods annotated with @Monitored will have their execution time tracked
 * and logged if they exceed the specified threshold.
 *
 * <p>Usage:
 * <pre>
 * &#064;Monitored(threshold = 200, description = "User lookup")
 * public User findUser(Long id) {
 *     // ...
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Monitored {

    /**
     * Threshold in milliseconds. Executions exceeding this will be logged as warnings.
     * Default is 100ms.
     */
    long threshold() default 100;

    /**
     * Optional description for logging purposes.
     */
    String description() default "";

    /**
     * Whether to include method arguments in logs.
     * Default is false for security/performance reasons.
     */
    boolean includeArgs() default false;

    /**
     * Whether to include return value type in logs.
     */
    boolean includeResult() default false;
}
