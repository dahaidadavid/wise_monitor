package com.dave.android.wiz_core.services.concurrency;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
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

/**
 * @author rendawei
 * @date 2018/6/5
 */
public abstract class AsyncTask<Params, Progress, Result> {

    private static final String LOG_TAG = "AsyncTask";
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE;
    private static final int MAXIMUM_POOL_SIZE;
    private static final int KEEP_ALIVE = 1;
    private static final ThreadFactory threadFactory;
    private static final BlockingQueue<Runnable> poolWorkQueue;
    public static final Executor THREAD_POOL_EXECUTOR;
    public static final Executor SERIAL_EXECUTOR;
    private static final int MESSAGE_POST_RESULT = 1;
    private static final int MESSAGE_POST_PROGRESS = 2;
    private static final AsyncTask.InternalHandler handler;
    private static volatile Executor defaultExecutor;
    private final AsyncTask.WorkerRunnable<Params, Result> worker;
    private final FutureTask<Result> future;
    private volatile AsyncTask.Status status;
    private final AtomicBoolean cancelled;
    private final AtomicBoolean taskInvoked;

    public static void init() {
        handler.getLooper();
    }

    public static void setDefaultExecutor(Executor exec) {
        defaultExecutor = exec;
    }

    public AsyncTask() {
        this.status = AsyncTask.Status.PENDING;
        this.cancelled = new AtomicBoolean();
        this.taskInvoked = new AtomicBoolean();
        this.worker = new AsyncTask.WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                AsyncTask.this.taskInvoked.set(true);
                Process.setThreadPriority(10);
                return AsyncTask.this.postResult(AsyncTask.this.doInBackground(this.params));
            }
        };
        this.future = new FutureTask<Result>(this.worker) {
            protected void done() {
                try {
                    AsyncTask.this.postResultIfNotInvoked(this.get());
                } catch (InterruptedException var2) {
                    Log.w(LOG_TAG, var2);
                } catch (ExecutionException var3) {
                    throw new RuntimeException("An error occured while executing doInBackground()", var3.getCause());
                } catch (CancellationException var4) {
                    AsyncTask.this.postResultIfNotInvoked(null);
                }

            }
        };
    }

    private void postResultIfNotInvoked(Result result) {
        boolean wasTaskInvoked = this.taskInvoked.get();
        if (!wasTaskInvoked) {
            this.postResult(result);
        }

    }

    private Result postResult(Result result) {
        Message message = handler.obtainMessage(MESSAGE_POST_RESULT, new AsyncTask.AsyncTaskResult(this, new Object[]{result}));
        message.sendToTarget();
        return result;
    }

    public final AsyncTask.Status getStatus() {
        return this.status;
    }

    protected abstract Result doInBackground(Params... var1);

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected void onProgressUpdate(Progress... values) {
    }

    protected void onCancelled(Result result) {
        this.onCancelled();
    }

    protected void onCancelled() {
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
            switch(this.status) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task: the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task: the task has already been executed (a task can be executed only once)");
            }
        }

        this.status = AsyncTask.Status.RUNNING;
        this.onPreExecute();
        this.worker.params = params;
        exec.execute(this.future);
        return this;
    }

    public static void execute(Runnable runnable) {
        defaultExecutor.execute(runnable);
    }

    protected final void publishProgress(Progress... values) {
        if (!this.isCancelled()) {
            handler.obtainMessage(MESSAGE_POST_PROGRESS, new AsyncTask.AsyncTaskResult(this, values)).sendToTarget();
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

    static {
        CORE_POOL_SIZE = CPU_COUNT + 1;
        MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
        threadFactory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, "AsyncTask #" + this.count.getAndIncrement());
            }
        };
        poolWorkQueue = new LinkedBlockingQueue<>(128);
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, poolWorkQueue, threadFactory);
        SERIAL_EXECUTOR = new AsyncTask.SerialExecutor(null);
        handler = new AsyncTask.InternalHandler();
        defaultExecutor = SERIAL_EXECUTOR;
    }

    private static class AsyncTaskResult<Data> {
        final AsyncTask task;
        final Data[] data;

        AsyncTaskResult(AsyncTask task, Data... data) {
            this.task = task;
            this.data = data;
        }
    }

    private abstract static class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] params;

        private WorkerRunnable() {
        }
    }

    private static class InternalHandler extends Handler {
        public InternalHandler() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            AsyncTask.AsyncTaskResult result = (AsyncTask.AsyncTaskResult)msg.obj;
            switch(msg.what) {
                case 1:
                    result.task.finish(result.data[0]);
                    break;
                case 2:
                    result.task.onProgressUpdate(result.data);
            }

        }
    }

    public enum Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    private static class SerialExecutor implements Executor {
        final LinkedList<Runnable> tasks;
        Runnable active;

        private SerialExecutor(Object o) {
            this.tasks = new LinkedList<>();
        }

        public synchronized void execute(final Runnable r) {
            this.tasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        SerialExecutor.this.scheduleNext();
                    }

                }
            });
            if (this.active == null) {
                this.scheduleNext();
            }

        }

        protected synchronized void scheduleNext() {
            if ((this.active =tasks.poll()) != null) {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(this.active);
            }

        }
    }
}
