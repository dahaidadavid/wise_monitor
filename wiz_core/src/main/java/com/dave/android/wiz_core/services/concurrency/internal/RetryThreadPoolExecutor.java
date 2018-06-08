package com.dave.android.wiz_core.services.concurrency.internal;

import com.dave.android.wiz_core.services.concurrency.rules.IBackoff;
import com.dave.android.wiz_core.services.concurrency.rules.IRetryPolicy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public class RetryThreadPoolExecutor extends ScheduledThreadPoolExecutor {
    private final IRetryPolicy retryPolicy;
    private final IBackoff backoff;

    public RetryThreadPoolExecutor(int corePoolSize, IRetryPolicy retryPolicy, IBackoff backoff) {
        this(corePoolSize, Executors.defaultThreadFactory(), retryPolicy, backoff);
    }

    public RetryThreadPoolExecutor(int corePoolSize, ThreadFactory factory, IRetryPolicy retryPolicy, IBackoff backoff) {
        super(corePoolSize, factory);
        if (retryPolicy == null) {
            throw new NullPointerException("retry policy must not be null");
        } else if (backoff == null) {
            throw new NullPointerException("backoff must not be null");
        } else {
            this.retryPolicy = retryPolicy;
            this.backoff = backoff;
        }
    }

    public Future<?> scheduleWithRetry(Runnable task) {
        return this.scheduleWithRetryInternal(Executors.callable(task));
    }

    public <T> Future<T> scheduleWithRetry(Runnable task, T result) {
        return this.scheduleWithRetryInternal(Executors.callable(task, result));
    }

    public <T> Future<T> scheduleWithRetry(Callable<T> task) {
        return this.scheduleWithRetryInternal(task);
    }

    private <T> Future<T> scheduleWithRetryInternal(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException();
        } else {
            RetryState retryState = new RetryState(this.backoff, this.retryPolicy);
            RetryFuture<T> retryFuture = new RetryFuture<>(task, retryState, this);
            this.execute(retryFuture);
            return retryFuture;
        }
    }

    public IRetryPolicy getRetryPolicy() {
        return this.retryPolicy;
    }

    public IBackoff getBackoff() {
        return this.backoff;
    }
}
