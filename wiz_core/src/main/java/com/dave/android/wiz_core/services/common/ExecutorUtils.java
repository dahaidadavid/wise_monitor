package com.dave.android.wiz_core.services.common;

import com.dave.android.wiz_core.services.concurrency.internal.Backoff;
import com.dave.android.wiz_core.services.concurrency.internal.RetryPolicy;
import com.dave.android.wiz_core.services.concurrency.internal.RetryThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public final class ExecutorUtils {

    private static final long DEFAULT_TERMINATION_TIMEOUT = 2L;

    private ExecutorUtils() {
    }

    public static ExecutorService buildSingleThreadExecutorService(String name) {
        ThreadFactory threadFactory = getNamedThreadFactory(name);
        ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        addDelayedShutdownHook(name, executor);
        return executor;
    }

    public static RetryThreadPoolExecutor buildRetryThreadPoolExecutor(String name, int corePoolSize, RetryPolicy retryPolicy, Backoff backoff) {
        ThreadFactory threadFactory = getNamedThreadFactory(name);
        RetryThreadPoolExecutor executor = new RetryThreadPoolExecutor(corePoolSize, threadFactory, retryPolicy, backoff);
        addDelayedShutdownHook(name, executor);
        return executor;
    }

    public static ScheduledExecutorService buildSingleThreadScheduledExecutorService(String name) {
        ThreadFactory threadFactory = getNamedThreadFactory(name);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        addDelayedShutdownHook(name, executor);
        return executor;
    }

    public static final ThreadFactory getNamedThreadFactory(final String threadNameTemplate) {
        final AtomicLong count = new AtomicLong(1L);
        return new ThreadFactory() {
            public Thread newThread(final Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(new BackgroundPriorityRunnable() {
                    public void onRun() {
                        runnable.run();
                    }
                });
                thread.setName(threadNameTemplate + count.getAndIncrement());
                return thread;
            }
        };
    }

    private static final void addDelayedShutdownHook(String serviceName, ExecutorService service) {
        addDelayedShutdownHook(serviceName, service, DEFAULT_TERMINATION_TIMEOUT, TimeUnit.SECONDS);
    }

    public static final void addDelayedShutdownHook(final String serviceName, final ExecutorService service, final long terminationTimeout, final TimeUnit timeUnit) {
        Runtime.getRuntime().addShutdownHook(new Thread(new BackgroundPriorityRunnable() {
            public void onRun() {
                try {
                    service.shutdown();
                    if (!service.awaitTermination(terminationTimeout, timeUnit)) {
                        service.shutdownNow();
                    }
                } catch (InterruptedException var2) {
                    service.shutdownNow();
                }

            }
        }, "Crashlytics Shutdown Hook for " + serviceName));
    }
}
