package com.dave.android.wiz_core.services.common;

import android.os.Process;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public abstract class BackgroundPriorityRunnable implements Runnable {
    public BackgroundPriorityRunnable() {
    }

    public final void run() {
        Process.setThreadPriority(10);
        this.onRun();
    }

    protected abstract void onRun();
}
