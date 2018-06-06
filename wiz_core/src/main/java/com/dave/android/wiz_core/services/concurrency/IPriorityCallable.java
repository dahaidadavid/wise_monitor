package com.dave.android.wiz_core.services.concurrency;

import java.util.concurrent.Callable;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public abstract class IPriorityCallable<V> extends IPriorityTask implements Callable<V> {
    public IPriorityCallable() {
    }
}
