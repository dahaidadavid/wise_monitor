package com.dave.android.wiz_core.services.concurrency.rules;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public interface IRetryPolicy {

    boolean shouldRetry(int var1, Throwable var2);
}
