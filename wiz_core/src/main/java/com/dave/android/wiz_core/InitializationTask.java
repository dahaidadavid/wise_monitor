package com.dave.android.wiz_core;

import com.dave.android.wiz_core.services.concurrency.Priority;
import com.dave.android.wiz_core.services.concurrency.AbsPriorityAsyncTask;

/**
 * @author rendawei
 * @date 2018/6/5
 */
class InitializationTask<Result> extends AbsPriorityAsyncTask<Void, Void, Result> {

    private static final String TIMING_METRIC_TAG = "KitInitialization";

    private final Kit<Result> kit;

    public InitializationTask(Kit<Result> kit) {
        this.kit = kit;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        TimingMetric timingMetric = createAndStartTimingMetric("onPreExecute");
        boolean result = false;
        try {
            result = kit.onPreExecute();
        } catch (Exception e) {
            throw e;
        } finally {
            timingMetric.stopMeasuring();
            if (!result) {
                cancel(true);
            }
        }
    }

    @Override
    protected Result doInBackground(Void... voids) {
        TimingMetric timingMetric = createAndStartTimingMetric("doInBackground");
        Result result = null;
        if (!isCancelled()) {
            result = kit.doInBackground();
        }
        timingMetric.stopMeasuring();
        return result;
    }

    @Override
    protected void onPostExecute(Result result) {
        kit.onPostExecute(result);
        kit.initializationCallback.success(result);
    }

    @Override
    protected void onCancelled(Result result) {
        kit.onCancelled(result);
        String message = kit.getIdentifier() + " Initialization was cancelled";
        InitializationException exception = new InitializationException(message);
        kit.initializationCallback.failure(exception);
    }

    public Priority getPriority() {
        return Priority.HIGH;
    }

    private TimingMetric createAndStartTimingMetric(String event) {
        TimingMetric timingMetric = new TimingMetric(kit.getIdentifier() + "." + event,
                TIMING_METRIC_TAG);
        timingMetric.startMeasuring();
        return timingMetric;
    }

}
