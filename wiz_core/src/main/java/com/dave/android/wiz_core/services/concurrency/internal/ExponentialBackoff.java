package com.dave.android.wiz_core.services.concurrency.internal;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public class ExponentialBackoff implements Backoff {
    private static final int DEFAULT_POWER = 2;
    private final long baseTimeMillis;
    private final int power;

    public ExponentialBackoff(long baseTimeMillis) {
        this(baseTimeMillis, 2);
    }

    public ExponentialBackoff(long baseTimeMillis, int power) {
        this.baseTimeMillis = baseTimeMillis;
        this.power = power;
    }

    public long getDelayMillis(int retries) {
        return (long)((double)this.baseTimeMillis * Math.pow((double)this.power, (double)retries));
    }
}
