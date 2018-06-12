package com.dave.android.wiz_core.services.concurrency.executor;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.concurrent.Executor;

/**
 * @author rendawei
 * @since 2018/6/6
 */
public class MainThreadExecutor implements Executor {

    private Handler mainHandler;

    public MainThreadExecutor(Handler handler) {
        if (handler != null) {
            this.mainHandler = handler;
        } else {
            this.mainHandler = new Handler(Looper.getMainLooper());
        }
    }

    @Override
    public void execute(Runnable command) {
        mainHandler.post(command);
    }
}