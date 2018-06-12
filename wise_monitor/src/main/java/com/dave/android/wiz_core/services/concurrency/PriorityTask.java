package com.dave.android.wiz_core.services.concurrency;

import android.support.annotation.NonNull;
import com.dave.android.wiz_core.services.concurrency.rules.IDependency;
import com.dave.android.wiz_core.services.concurrency.rules.IPriorityProvider;
import com.dave.android.wiz_core.services.concurrency.rules.ITask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 具有优先级的任务基类
 *
 * @author rendawei
 * @since 2018/6/5
 */
public class PriorityTask implements IDependency<ITask>, IPriorityProvider, ITask {

    private final List<ITask> dependencies = new ArrayList<>();
    private final AtomicBoolean hasRun = new AtomicBoolean(false);
    private final AtomicReference<Throwable> throwable = new AtomicReference<>(null);

    public PriorityTask() {
    }

    @Override
    public synchronized Collection<ITask> getDependencies() {
        return Collections.unmodifiableCollection(dependencies);
    }

    @Override
    public synchronized void addDependency(ITask task) {
        dependencies.add(task);
    }

    @Override
    public boolean areDependenciesFinished() {
        Iterator iterator = getDependencies().iterator();
        ITask task;
        do {
            if (!iterator.hasNext()) {
                return true;
            }
            task = (ITask) iterator.next();
        } while (task.isFinished());

        return false;
    }

    @Override
    public synchronized void setFinished(boolean finished) {
        hasRun.set(finished);
    }

    @Override
    public boolean isFinished() {
        return hasRun.get();
    }

    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    @Override
    public void setError(Throwable throwable) {
        this.throwable.set(throwable);
    }

    @Override
    public Throwable getError() {
        return throwable.get();
    }

    public int compareTo(@NonNull Object other) {
        return Priority.compareTo(this, other);
    }

    /**
     * 判断对象是否同时是 <code>IDependency & ITask & IPriorityProvider</> 的子类
     */
    public static boolean isProperDelegate(Object object) {
        try {
            IDependency<ITask> dep = (IDependency) object;
            ITask task = (ITask) object;
            IPriorityProvider provider = (IPriorityProvider) object;
            return dep != null && task != null && provider != null;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return false;
        }
    }
}
