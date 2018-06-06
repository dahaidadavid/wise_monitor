package com.dave.android.wiz_core.services.concurrency.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author rendawei
 * @date 2018/6/5
 */
class RetryFuture<T> extends AbstractFuture<T> implements Runnable {

    private final RetryThreadPoolExecutor executor;
    private final Callable<T> task;
    private final AtomicReference<Thread> runner;
    RetryState retryState;

    RetryFuture(Callable<T> task, RetryState retryState, RetryThreadPoolExecutor executor) {
        this.task = task;
        this.retryState = retryState;
        this.executor = executor;
        this.runner = new AtomicReference<>();
    }

    public void run() {
        if (!this.isDone() && this.runner.compareAndSet(null, Thread.currentThread())) {
            try {
                T result = this.task.call();
                this.set(result);
            } catch (Throwable var7) {
                if (this.getRetryPolicy().shouldRetry(this.getRetryCount(), var7)) {
                    long delay = this.getBackoff().getDelayMillis(this.getRetryCount());
                    this.retryState = this.retryState.nextRetryState();
                    this.executor.schedule(this, delay, TimeUnit.MILLISECONDS);
                } else {
                    this.setException(var7);
                }
            } finally {
                this.runner.getAndSet(null);
            }

        }
    }

    private RetryPolicy getRetryPolicy() {
        return this.retryState.getRetryPolicy();
    }

    private Backoff getBackoff() {
        return this.retryState.getBackoff();
    }

    private int getRetryCount() {
        return this.retryState.getRetryCount();
    }

    protected void interruptTask() {
        Thread thread = (Thread) this.runner.getAndSet(null);
        if (thread != null) {
            thread.interrupt();
        }

    }
}
