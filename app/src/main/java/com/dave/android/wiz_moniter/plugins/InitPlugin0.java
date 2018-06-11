package com.dave.android.wiz_moniter.plugins;

import android.os.Looper;
import android.util.Log;
import com.dave.android.wiz_core.Kit;
import com.dave.android.wiz_core.services.concurrency.AsyncTask;
import com.dave.android.wiz_core.services.concurrency.KitConfig;

/**
 * @author rendawei
 * @date 2018/6/5
 */
@KitConfig(thread = AsyncTask.THREAD_MAIN)
public class InitPlugin0 extends Kit {

    private static final String TAG = InitPlugin0.class.getSimpleName();

    @Override
    public String getIdentifier() {
        return TAG;
    }

    @Override
    protected Object doInBackground() {
        Log.e(TAG, "InitPlugin0 doInBackground");
        if(Thread.currentThread() != Looper.getMainLooper().getThread()){
            Log.e(TAG, "InitPlugin0 Must init in main thread");
            throw new RuntimeException("Must init in main thread");
        }
        return null;
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }
}
