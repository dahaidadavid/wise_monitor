package com.dave.android.wiz_core.services.concurrency;

import android.support.annotation.NonNull;
import com.dave.android.wiz_core.services.concurrency.rules.IDelegateProvider;
import com.dave.android.wiz_core.services.concurrency.rules.IDependency;
import com.dave.android.wiz_core.services.concurrency.rules.IPriorityProvider;
import com.dave.android.wiz_core.services.concurrency.rules.ITask;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public class PriorityFutureTask<V> extends FutureTask<V> implements IDependency<ITask>, IPriorityProvider, ITask, IDelegateProvider {

    private final Object delegate;

    public PriorityFutureTask(Callable<V> callable) {
        super(callable);
        this.delegate = checkAndInitDelegate(callable);
    }

    public PriorityFutureTask(Runnable runnable, V result) {
        super(runnable, result);
        this.delegate = checkAndInitDelegate(runnable);
    }

    @Override
    public int compareTo(@NonNull Object another) {
        return getDelegate().compareTo(another);
    }

    @Override
    public void addDependency(ITask task) {
        getDelegate().addDependency(task);
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
        return (T) delegate;
    }

    private  <T extends IDependency<ITask> & IPriorityProvider & ITask> T checkAndInitDelegate(Object object) {
        return (T) (PriorityTask.isProperDelegate(object) ? (IDependency) object : new PriorityTask());
    }
}
