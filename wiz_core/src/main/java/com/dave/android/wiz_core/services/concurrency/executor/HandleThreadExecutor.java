package com.dave.android.wiz_core.services.concurrency.executor;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.concurrent.Executor;

/**
 * @author rendawei
 * @date 2018/6/6
 */
public class HandleThreadExecutor implements Executor {

    private Handler handler;

    public HandleThreadExecutor(@Nullable Handler handler) {
        if (handler != null) {
            this.handler = handler;
        } else {
            HandlerThread mHandlerThread = new HandlerThread("HandleThreadExecutor");
            mHandlerThread.run();
            this.handler = new Handler(handler.getLooper());
        }
    }

    @Override
    public void execute(@NonNull Runnable command) {
        handler.post(command);
    }
}
