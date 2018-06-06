package com.dave.android.wiz_core.services.concurrency;

import android.annotation.TargetApi;
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
 * @date 2018/6/5
 */
public class PriorityThreadPoolExecutor extends ThreadPoolExecutor {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE;
    private static final int MAXIMUM_POOL_SIZE;
    private static final long KEEP_ALIVE = 1L;

    <T extends Runnable & IDependency & ITask & IPriorityProvider> PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, DependencyPriorityBlockingQueue<T> workQueue, ThreadFactory factory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, (BlockingQueue) workQueue, factory);
        this.prestartAllCoreThreads();
    }

    public static <T extends Runnable & IDependency & ITask & IPriorityProvider> PriorityThreadPoolExecutor create(
            int corePoolSize, int maxPoolSize) {
        return new PriorityThreadPoolExecutor(corePoolSize, maxPoolSize, KEEP_ALIVE, TimeUnit.SECONDS,
                new DependencyPriorityBlockingQueue(),
                new PriorityThreadPoolExecutor.PriorityThreadFactory(10));
    }

    public static PriorityThreadPoolExecutor create(int threadCount) {
        return create(threadCount, threadCount);
    }

    public static PriorityThreadPoolExecutor create() {
        return create(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE);
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new IPriorityFutureTask<>(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new IPriorityFutureTask<>(callable);
    }

    @TargetApi(9)
    public void execute(Runnable command) {
        if (IPriorityTask.isProperDelegate(command)) {
            super.execute(command);
        } else {
            super.execute(this.newTaskFor(command, null));
        }

    }

    protected void afterExecute(Runnable runnable, Throwable throwable) {
        ITask task = (ITask) runnable;
        task.setFinished(true);
        task.setError(throwable);
        this.getQueue().recycleBlockedQueue();
        super.afterExecute(runnable, throwable);
    }

    public DependencyPriorityBlockingQueue getQueue() {
        return (DependencyPriorityBlockingQueue) super.getQueue();
    }

    static {
        CORE_POOL_SIZE = CPU_COUNT + 1;
        MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    }

    protected static final class PriorityThreadFactory implements ThreadFactory {

        private final int threadPriority;

        public PriorityThreadFactory(int threadPriority) {
            this.threadPriority = threadPriority;
        }

        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(this.threadPriority);
            thread.setName("Queue");
            return thread;
        }
    }
}
