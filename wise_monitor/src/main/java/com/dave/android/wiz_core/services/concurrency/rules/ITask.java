package com.dave.android.wiz_core.services.concurrency.rules;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public interface ITask {

    void setFinished(boolean var1);

    boolean isFinished();

    void setError(Throwable var1);

    Throwable getError();
}
