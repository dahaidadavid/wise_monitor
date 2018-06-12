package com.dave.android.wiz_moniter.plugins;

import android.os.Looper;
import android.util.Log;
import com.dave.android.wiz_core.Kit;
import com.dave.android.wiz_core.services.concurrency.DependsOn;

/**
 * @author rendawei
 * @since 2018/6/5
 */
@DependsOn(value = {InitPlugin2.class})
public class InitPlugin1 extends Kit {

    private static final String TAG = InitPlugin1.class.getSimpleName();

    @Override
    public String getIdentifier() {
        return TAG;
    }

    @Override
    protected Object doInBackground() {
        Log.e(TAG, "InitPlugin1 doInBackground");
        if(Thread.currentThread() == Looper.getMainLooper().getThread()){
            Log.e(TAG, "InitPlugin1 Must init in main thread");
            throw new RuntimeException("Must not init in main thread");
        }
        return null;
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }
}
