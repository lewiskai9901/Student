package com.school.management.common.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    String module();
    String action();
    String resourceType();
    String description() default "";
}
