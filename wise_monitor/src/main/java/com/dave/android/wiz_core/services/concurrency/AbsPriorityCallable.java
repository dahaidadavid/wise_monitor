package com.dave.android.wiz_core.services.concurrency;

import java.util.concurrent.Callable;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public abstract class AbsPriorityCallable<V> extends PriorityTask implements Callable<V> {
    public AbsPriorityCallable() {
    }
}
