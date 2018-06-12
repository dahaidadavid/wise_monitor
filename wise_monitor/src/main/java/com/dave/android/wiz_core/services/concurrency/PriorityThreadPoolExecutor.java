package com.dave.android.wiz_core.services.concurrency;

import android.annotation.TargetApi;
import android.support.annotation.NonNull;
import com.dave.android.wiz_core.services.concurrency.rules.IDependency;
import com.dave.android.wiz_core.services.concurrency.rules.IPriorityProvider;
import com.dave.android.wiz_core.services.concurrency.rules.ITask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public class PriorityThreadPoolExecutor extends ThreadPoolExecutor {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE;
    private static final int MAXIMUM_POOL_SIZE;
    private static final long KEEP_ALIVE = 1L;

    static {
        CORE_POOL_SIZE = CPU_COUNT + 1;
        MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    }

    <T extends Runnable & IDependency & ITask & IPriorityProvider> PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, DependencyPriorityBlockingQueue<T> workQueue, ThreadFactory factory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, (BlockingQueue) workQueue, factory);
        this.prestartAllCoreThreads();
    }

    public static <T extends Runnable & IDependency & ITask & IPriorityProvider> PriorityThreadPoolExecutor create(int corePoolSize, int maxPoolSize) {
        return new PriorityThreadPoolExecutor(corePoolSize, maxPoolSize, KEEP_ALIVE, TimeUnit.SECONDS, new DependencyPriorityBlockingQueue<T>(), new PriorityThreadPoolExecutor.PriorityThreadFactory(10));
    }

    public static PriorityThreadPoolExecutor create(int threadCount) {
        return create(threadCount, threadCount);
    }

    public static PriorityThreadPoolExecutor create() {
        return create(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new PriorityFutureTask<>(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new PriorityFutureTask<>(callable);
    }

    @TargetApi(9)
    @Override
    public void execute(Runnable command) {
        if (PriorityTask.isProperDelegate(command)) {
            super.execute(command);
        } else {
            super.execute(this.newTaskFor(command, null));
        }
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        ITask task = (ITask) runnable;
        task.setFinished(true);
        task.setError(throwable);
        getQueue().recycleBlockedQueue();
        super.afterExecute(runnable, throwable);
    }

    @Override
    public DependencyPriorityBlockingQueue getQueue() {
        return (DependencyPriorityBlockingQueue) super.getQueue();
    }

    protected static final class PriorityThreadFactory implements ThreadFactory {

        private final int threadPriority;

        PriorityThreadFactory(int threadPriority) {
            this.threadPriority = threadPriority;
        }

        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(this.threadPriority);
            thread.setName("Queue");
            return thread;
        }
    }
}
