package com.dave.android.wiz_core.services.concurrency.internal;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public class DefaultRetryPolicy implements RetryPolicy {

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
