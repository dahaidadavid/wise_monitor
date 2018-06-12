package com.dave.android.wiz_core.services.concurrency.internal;

import com.dave.android.wiz_core.services.concurrency.rules.IRetryPolicy;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public class DefaultRetryPolicy implements IRetryPolicy {

    private final int maxRetries;

    public DefaultRetryPolicy() {
        this(1);
    }

    public DefaultRetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean shouldRetry(int retries, Throwable e) {
        return retries < maxRetries;
    }
}
