package com.dave.android.wiz_core.services.concurrency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author rendawei
 * @since 2018/6/5
 */
@Target(value = {ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KitConfig {

    String thread() default "thread";
}
