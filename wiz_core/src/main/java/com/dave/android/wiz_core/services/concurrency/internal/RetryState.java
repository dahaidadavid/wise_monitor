package com.dave.android.wiz_core.services.concurrency.internal;

import com.dave.android.wiz_core.services.concurrency.rules.IBackoff;
import com.dave.android.wiz_core.services.concurrency.rules.IRetryPolicy;

/**
 * @author rendawei
 * @date 2018/6/5
 */

public class RetryState {
    private final int retryCount;
    private final IBackoff backoff;
    private final IRetryPolicy retryPolicy;

    public RetryState(IBackoff backoff, IRetryPolicy retryPolicy) {
        this(0, backoff, retryPolicy);
    }

    public RetryState(int retryCount, IBackoff backoff, IRetryPolicy retryPolicy) {
        this.retryCount = retryCount;
        this.backoff = backoff;
        this.retryPolicy = retryPolicy;
    }

    public int getRetryCount() {
        return this.retryCount;
    }

    public long getRetryDelay() {
        return this.backoff.getDelayMillis(this.retryCount);
    }

    public IBackoff getBackoff() {
        return this.backoff;
    }

    public IRetryPolicy getRetryPolicy() {
        return this.retryPolicy;
    }

    public RetryState nextRetryState() {
        return new RetryState(this.retryCount + 1, this.backoff, this.retryPolicy);
    }

    public RetryState initialRetryState() {
        return new RetryState(this.backoff, this.retryPolicy);
    }
}
