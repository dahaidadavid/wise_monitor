package com.dave.android.wiz_core;

import android.content.Context;
import android.support.annotation.NonNull;
import com.dave.android.wiz_core.services.concurrency.AsyncTask;
import com.dave.android.wiz_core.services.concurrency.DependsOn;
import com.dave.android.wiz_core.services.concurrency.KitConfig;
import com.dave.android.wiz_core.services.concurrency.executor.HandleThreadExecutor;
import com.dave.android.wiz_core.services.concurrency.executor.MainThreadExecutor;
import com.dave.android.wiz_core.services.concurrency.rules.ITask;
import java.io.File;
import java.util.Collection;

/**
 * 需要初始化的任务的基类
 *
 * @author rendawei
 * @since 2018/6/5
 */
public abstract class Kit<Result> implements Comparable<Kit> {

    private WiseInitCenter mWiseInitCenter;
    private Context context;

    InitializationTask<Result> initializationTask = new InitializationTask<>(this);
    InitializationCallback<Result> initializationCallback;

    final DependsOn dependsOnAnnotation = getClass().getAnnotation(DependsOn.class);
    final KitConfig kitConfigAnnotation = getClass().getAnnotation(KitConfig.class);

    public Kit() {
    }

    void injectParameters(Context context, WiseInitCenter wiseInitCenter, InitializationCallback<Result> callback) {
        this.mWiseInitCenter = wiseInitCenter;
        this.context = new WiseContext(context, getIdentifier(), getPath());
        this.initializationCallback = callback;
    }

    final void initialize() {
        initializationTask.executeOnExecutor(mWiseInitCenter.getExecutorService(), new Void[]{null});
    }

    public Context getContext() {
        return context;
    }

    public WiseInitCenter getWiseInitCenter() {
        return mWiseInitCenter;
    }

    public String getPath() {
        return ".WiseInitCenter" + File.separator + getIdentifier();
    }

    @Override
    public int compareTo(@NonNull Kit another) {
        if (containsAnnotatedDependency(another)) {
            return 1;
        } else if (another.containsAnnotatedDependency(this)) {
            return -1;
        } else if (hasAnnotatedDependency() && !another.hasAnnotatedDependency()) {
            return 1;
        } else {
            return !hasAnnotatedDependency() && another.hasAnnotatedDependency() ? -1 : 0;
        }
    }

    boolean containsAnnotatedDependency(Kit target) {
        if (hasAnnotatedDependency()) {
            Class<?>[] dependencies = dependsOnAnnotation.value();
            for (Class<?> dep : dependencies) {
                if (dep.isAssignableFrom(target.getClass())) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean hasAnnotatedDependency() {
        return dependsOnAnnotation != null;
    }

    boolean hasAnnotatedKitConfig() {
        return kitConfigAnnotation != null;
    }

    String validateAnnotationThread() {
        if (hasAnnotatedKitConfig()) {
            return kitConfigAnnotation.thread();
        }
        return AsyncTask.THREAD_BACKGROUND;
    }

    protected Collection<ITask> getDependencies() {
        return initializationTask.getDependencies();
    }


    public abstract String getIdentifier();

    public abstract String getVersion();

    /**
     * 任务执行前回调，初始化
     *
     * @return true if ready
     */
    protected boolean onPreExecute() {
        return true;
    }

    /**
     * 开始执行初始化
     *
     * @return Result 执行结果
     */
    protected abstract Result doInBackground();

    protected void onPostExecute(Result result) {
    }

    protected void onCancelled(Result result) {
    }

}
