package com.dave.android.wiz_moniter.plugins;

import android.util.Log;
import com.dave.android.wiz_core.Kit;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public class InitPlugin0 extends Kit {

    private static final String TAG = InitPlugin0.class.getSimpleName();

    @Override
    public String getIdentifier() {
        return TAG;
    }

    @Override
    protected Object doInBackground() {
        Log.e(TAG, "InitPlugin0 doInBackground");
        return null;
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }
}
