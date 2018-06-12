package com.dave.android.wiz_moniter.plugins;

import android.util.Log;
import com.dave.android.wiz_core.Kit;
import com.dave.android.wiz_core.services.concurrency.AsyncTask;
import com.dave.android.wiz_core.services.concurrency.DependsOn;
import com.dave.android.wiz_core.services.concurrency.KitConfig;

/**
 * @author rendawei
 * @since 2018/6/5
 */
@KitConfig(thread = AsyncTask.THREAD_HANDLER)
@DependsOn(value = InitPlugin0.class)
public class InitPlugin2 extends Kit {

    private static final String TAG = InitPlugin2.class.getSimpleName();

    @Override
    public String getIdentifier() {
        return TAG;
    }

    @Override
    protected Object doInBackground() {
        Log.e(TAG, "InitPlugin2 doInBackground");
        return null;
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

}
