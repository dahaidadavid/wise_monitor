package com.dave.android.wiz_core.services.concurrency.internal;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public abstract class AbstractFuture<V> implements Future<V> {

    private final Sync<V> sync = new Sync<>();

    protected AbstractFuture() {
    }

    static final CancellationException cancellationExceptionWithCause(String message, Throwable cause) {
        CancellationException exception = new CancellationException(message);
        exception.initCause(cause);
        return exception;
    }

    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, ExecutionException {
        return sync.get(unit.toNanos(timeout));
    }

    public V get() throws InterruptedException, ExecutionException {
        return sync.get();
    }

    public boolean isDone() {
        return sync.isDone();
    }

    public boolean isCancelled() {
        return sync.isCancelled();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!sync.cancel(mayInterruptIfRunning)) {
            return false;
        } else {
            if (mayInterruptIfRunning) {
                this.interruptTask();
            }

            return true;
        }
    }

    protected void interruptTask() {
    }

    protected final boolean wasInterrupted() {
        return sync.wasInterrupted();
    }

    protected boolean set(V value) {
        return sync.set(value);
    }

    protected boolean setException(Throwable throwable) {
        if (throwable == null) {
            throw new NullPointerException();
        } else {
            return sync.setException(throwable);
        }
    }

    static final class Sync<V> extends AbstractQueuedSynchronizer {

        static final int RUNNING = 0;
        static final int COMPLETING = 1;
        static final int COMPLETED = 2;
        static final int CANCELLED = 4;
        static final int INTERRUPTED = 8;
        private static final long serialVersionUID = 0L;
        private V value;
        private Throwable exception;

        Sync() {
        }

        protected int tryAcquireShared(int ignored) {
            return this.isDone() ? 1 : -1;
        }

        protected boolean tryReleaseShared(int finalState) {
            this.setState(finalState);
            return true;
        }

        V get(long nanos)
                throws TimeoutException, CancellationException, ExecutionException, InterruptedException {
            if (!this.tryAcquireSharedNanos(-1, nanos)) {
                throw new TimeoutException("Timeout waiting for task.");
            } else {
                return this.getValue();
            }
        }

        V get() throws CancellationException, ExecutionException, InterruptedException {
            this.acquireSharedInterruptibly(-1);
            return this.getValue();
        }

        private V getValue() throws CancellationException, ExecutionException {
            int state = this.getState();
            switch (state) {
                case COMPLETED:
                    if (this.exception != null) {
                        throw new ExecutionException(this.exception);
                    }

                    return this.value;
                case CANCELLED:
                case INTERRUPTED:
                    throw AbstractFuture.cancellationExceptionWithCause("Task was cancelled.", this.exception);
                default:
                    throw new IllegalStateException("Error, synchronizer in invalid state: " + state);
            }
        }

        boolean isDone() {
            return (this.getState() & 14) != RUNNING;
        }

        boolean isCancelled() {
            return (this.getState() & 12) != RUNNING;
        }

        boolean wasInterrupted() {
            return this.getState() == INTERRUPTED;
        }

        boolean set(V v) {
            return this.complete(v, null, COMPLETED);
        }

        boolean setException(Throwable t) {
            return this.complete(null, t, COMPLETED);
        }

        boolean cancel(boolean interrupt) {
            return this.complete(null, null, interrupt ? INTERRUPTED : CANCELLED);
        }

        private boolean complete(V v, Throwable t, int finalState) {
            boolean doCompletion = this.compareAndSetState(RUNNING, COMPLETING);
            if (doCompletion) {
                this.value = v;
                this.exception = ((finalState & 12) != RUNNING ? new CancellationException("Future.cancel() was called.") : t);
                this.releaseShared(finalState);
            } else if (this.getState() == COMPLETING) {
                this.acquireShared(-1);
            }

            return doCompletion;
        }
    }
}
