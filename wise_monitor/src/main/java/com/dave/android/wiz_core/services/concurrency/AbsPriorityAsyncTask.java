package com.dave.android.wiz_core.services.concurrency;

import android.support.annotation.NonNull;
import com.dave.android.wiz_core.services.concurrency.rules.IDelegateProvider;
import com.dave.android.wiz_core.services.concurrency.rules.IDependency;
import com.dave.android.wiz_core.services.concurrency.rules.IPriorityProvider;
import com.dave.android.wiz_core.services.concurrency.rules.ITask;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public abstract class AbsPriorityAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements IDependency<ITask>, IPriorityProvider, ITask, IDelegateProvider {

    private final PriorityTask priorityTask = new PriorityTask();

    public AbsPriorityAsyncTask() {
    }

    @Override
    public int compareTo(Object another) {
        return Priority.compareTo(this, another);
    }

    @Override
    public void addDependency(ITask task) {
        if (getStatus() != Status.PENDING) {
            throw new IllegalStateException("Shouldn't add IDependency after task is running");
        } else {
            getDelegate().addDependency(task);
        }
    }

    @Override
    public Collection<ITask> getDependencies() {
        return getDelegate().getDependencies();
    }

    @Override
    public boolean areDependenciesFinished() {
        return getDelegate().areDependenciesFinished();
    }

    @Override
    public Priority getPriority() {
        return getDelegate().getPriority();
    }

    @Override
    public void setFinished(boolean finished) {
        getDelegate().setFinished(finished);
    }

    @Override
    public boolean isFinished() {
        return getDelegate().isFinished();
    }

    @Override
    public void setError(Throwable throwable) {
        getDelegate().setError(throwable);
    }

    @Override
    public Throwable getError() {
        return getDelegate().getError();
    }

    @Override
    public <T extends IDependency<ITask> & IPriorityProvider & ITask> T getDelegate() {
        return (T) priorityTask;
    }

    public final void executeOnExecutor(ExecutorService exec, Params... params) {
        Executor executor = new ProxyExecutor(exec, this);
        super.executeOnExecutor(executor, params);
    }

    private static class ProxyExecutor<Result> implements Executor {

        private final Executor executor;
        private final AbsPriorityAsyncTask task;

        ProxyExecutor(Executor ex, AbsPriorityAsyncTask task) {
            this.executor = ex;
            this.task = task;
        }

        public void execute(@NonNull Runnable command) {
            executor.execute(new PriorityFutureTask<Result>(command, null) {
                @Override
                public <T extends IDependency<ITask> & IPriorityProvider & ITask> T getDelegate() {
                    return (T) task;
                }
            });
        }
    }
}
