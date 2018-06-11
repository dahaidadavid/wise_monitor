package com.dave.android.wiz_core.services.concurrency;

import android.support.annotation.StringDef;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author rendawei
 * @date 2018/6/5
 */
@Target(value = {ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@StringDef({AsyncTask.THREAD_BACKGROUND,AsyncTask.THREAD_MAIN,AsyncTask.THREAD_HANDLER})
public @interface KitConfig {

    String thread() default "thread";
}
