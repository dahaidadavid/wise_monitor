package com.dave.android.wiz_core.services.concurrency.internal;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public interface Backoff {
    long getDelayMillis(int var1);
}
