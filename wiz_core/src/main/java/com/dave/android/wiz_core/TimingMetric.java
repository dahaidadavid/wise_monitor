package com.dave.android.wiz_core;

import android.os.SystemClock;
import android.util.Log;

/**
 * 事件耗时统计
 *
 * @author rendawei
 * @date 2018/6/5
 */
public class TimingMetric {

    private final String eventName;
    private final String tag;
    private final boolean disabled;
    private long start;
    private long duration;

    public TimingMetric(String eventName, String tag) {
        this.eventName = eventName;
        this.tag = tag;
        this.disabled = !Log.isLoggable(tag, Log.VERBOSE);
    }

    public synchronized void startMeasuring() {
        if (!this.disabled) {
            this.start = SystemClock.elapsedRealtime();
            this.duration = 0L;
        }
    }

    public synchronized void stopMeasuring() {
        if (!this.disabled) {
            if (this.duration == 0L) {
                this.duration = SystemClock.elapsedRealtime() - this.start;
                this.reportToLog();
            }
        }
    }

    public long getDuration() {
        return this.duration;
    }

    private void reportToLog() {
        Log.v(this.tag, this.eventName + ": " + this.duration + "ms");
    }
}
