package com.dave.android.wiz_core.services.concurrency.executor;

import android.os.Handler;
import android.os.HandlerThread;
import java.util.concurrent.Executor;

/**
 * @author rendawei
 * @since 2018/6/6
 */
public class HandleThreadExecutor implements Executor {

    private Handler handler;

    public HandleThreadExecutor(Handler handler) {
        if (handler != null) {
            this.handler = handler;
        } else {
            HandlerThread mHandlerThread = new HandlerThread("HandleThreadExecutor");
            mHandlerThread.start();
            this.handler = new Handler(mHandlerThread.getLooper());
        }
    }

    @Override
    public void execute(Runnable command) {
        handler.post(command);
    }
}