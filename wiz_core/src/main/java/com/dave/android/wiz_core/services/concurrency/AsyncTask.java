package com.dave.android.wiz_core.services.concurrency;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.StringDef;
import android.util.Log;
import com.dave.android.wiz_core.services.concurrency.executor.ExecutorUtils;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异步任务执行器
 *
 * @author rendawei
 * @date 2018/6/5
 */
public abstract class AsyncTask<Params, Progress, Result> {

    private static final String LOG_TAG = "AsyncTask";

    /**
     * Used For defined which thread to executor the task {@link KitConfig}
     */
    public static final String THREAD_BACKGROUND = "background";
    public static final String THREAD_MAIN = "main";
    public static final String THREAD_HANDLER = "threadHandler";

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE;
    private static final int MAXIMUM_POOL_SIZE;
    private static final int KEEP_ALIVE = 1;

    private static final ThreadFactory threadFactory;
    private static final BlockingQueue<Runnable> poolWorkQueue;
    private static final Executor THREAD_POOL_EXECUTOR;
    private static final Executor SERIAL_EXECUTOR;
    private static final int MESSAGE_POST_RESULT = 1;
    private static final int MESSAGE_POST_PROGRESS = 2;
    private static final InternalHandler handler;
    private static volatile Executor defaultExecutor;

    private final WorkerRunnable<Params, Result> worker;
    private final FutureTask<Result> future;
    private final AtomicBoolean cancelled;
    private final AtomicBoolean taskInvoked;
    private final AtomicReference<String> currentRunningThread;

    private volatile AsyncTask.Status status;

    static {
        CORE_POOL_SIZE = CPU_COUNT + 1;
        MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
        threadFactory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, "Init AsyncTask #" + this.count.getAndIncrement());
            }
        };
        poolWorkQueue = new LinkedBlockingQueue<>(128);
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                TimeUnit.SECONDS, poolWorkQueue, threadFactory);

        handler = new InternalHandler();
        SERIAL_EXECUTOR = new SerialExecutor();
        defaultExecutor = SERIAL_EXECUTOR;
    }

    private static class InternalHandler extends Handler {

        InternalHandler() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            AsyncTaskResult result = (AsyncTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    result.task.finish(result.result);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.task.onProgressUpdate(result.data);
                    break;
                default:
                    break;
            }
        }
    }

    public AsyncTask() {
        this.status = AsyncTask.Status.PENDING;
        this.cancelled = new AtomicBoolean();
        this.taskInvoked = new AtomicBoolean();
        this.currentRunningThread = new AtomicReference<>(THREAD_BACKGROUND);
        this.worker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                Process.setThreadPriority(10);
                Result runResult = null;
                taskInvoked.set(true);
                switch (currentRunningThread.get()){
                    case THREAD_BACKGROUND:
                        runResult = doInBackground(this.params);
                        postResult(runResult);
                        mainLock.countDown();
                        break;
                    case THREAD_HANDLER:
                        ExecutorUtils.getHandleThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                postResult(doInBackground(params));
                                mainLock.countDown();
                            }
                        });
                        break;
                    case THREAD_MAIN:
                        ExecutorUtils.getMainThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                postResult(doInBackground(params));
                                mainLock.countDown();
                            }
                        });
                        break;
                    default:
                            break;
                }
                mainLock.await();
                return runResult;
            }
        };
        this.future = new FutureTask<Result>(this.worker) {
            protected void done() {
                try {
                    postResultIfNotInvoked(AsyncTask.this.get());
                } catch (InterruptedException var2) {
                    Log.w(LOG_TAG, var2);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()", e.getCause());
                } catch (CancellationException var4) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    public static void setDefaultExecutor(Executor exec) {
        defaultExecutor = exec;
    }

    public void setCurrentRunningThread(String thread){
        this.currentRunningThread.set(thread);
    }

    private void postResultIfNotInvoked(Result result) {
        boolean wasTaskInvoked = this.taskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    private Result postResult(Result result) {
        Message message = handler.obtainMessage(MESSAGE_POST_RESULT, new AsyncTaskResult<>(this, result));
        message.sendToTarget();
        return result;
    }

    public final AsyncTask.Status getStatus() {
        return this.status;
    }

    public final boolean isCancelled() {
        return this.cancelled.get();
    }

    public final boolean cancel(boolean mayInterruptIfRunning) {
        this.cancelled.set(true);
        return this.future.cancel(mayInterruptIfRunning);
    }

    public final Result get() throws InterruptedException, ExecutionException {
        return this.future.get();
    }

    public final Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.future.get(timeout, unit);
    }

    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return this.executeOnExecutor(defaultExecutor, params);
    }

    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec, Params... params) {
        if (this.status != AsyncTask.Status.PENDING) {
            switch (this.status) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task: the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task: the task has already been executed (a task can be executed only once)");
                default:
                    break;
            }
        }

        this.status = AsyncTask.Status.RUNNING;
        this.onPreExecute();
        this.worker.params = params;
        exec.execute(this.future);
        return this;
    }

    protected final void publishProgress(Progress... values) {
        if (!this.isCancelled()) {
            handler.obtainMessage(MESSAGE_POST_PROGRESS, new AsyncTaskResult<>(this, null, values)).sendToTarget();
        }
    }

    private void finish(Result result) {
        if (this.isCancelled()) {
            this.onCancelled(result);
        } else {
            this.onPostExecute(result);
        }

        this.status = AsyncTask.Status.FINISHED;
    }

    protected void onPreExecute() {}

    protected abstract Result doInBackground(Params... var1);

    protected void onProgressUpdate(Progress... values) {}

    protected void onPostExecute(Result result) {}

    protected void onCancelled(Result result) {
        this.onCancelled();
    }

    protected void onCancelled() {
    }

    /**
     * 异步结果
     * @param <Result>
     * @param <Progress>
     */
    private static class AsyncTaskResult<Result,Progress> {

        final AsyncTask task;
        final Result result;
        final Progress[] data;

        AsyncTaskResult(AsyncTask task,Result result, Progress... data) {
            this.task = task;
            this.result = result;
            this.data = data;
        }
    }

    private abstract static class WorkerRunnable<Params, Result> implements Callable<Result> {
        CountDownLatch mainLock = new CountDownLatch(1);
        Params[] params;
    }

    public enum Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    /**
     * 序列化执行器，内部维护一个队列来顺序执行任务
     */
    private static class SerialExecutor implements Executor {

        final LinkedList<Runnable> tasks;
        private Runnable active;

        private SerialExecutor() {
            this.tasks = new LinkedList<>();
        }

        public synchronized void execute(final Runnable r) {
            tasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (active == null) {
                scheduleNext();
            }

        }

        synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(active);
            }
        }
    }
}
