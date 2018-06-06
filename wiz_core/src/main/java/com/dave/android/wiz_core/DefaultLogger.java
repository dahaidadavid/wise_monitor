package com.dave.android.wiz_core;

import android.util.Log;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public class DefaultLogger implements Logger {
    private int logLevel;

    public DefaultLogger(int logLevel) {
        this.logLevel = logLevel;
    }

    public DefaultLogger() {
        this.logLevel = 4;
    }

    public boolean isLoggable(String tag, int level) {
        return this.logLevel <= level;
    }

    public int getLogLevel() {
        return this.logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public void d(String tag, String text, Throwable throwable) {
        if (this.isLoggable(tag, 3)) {
            Log.d(tag, text, throwable);
        }

    }

    public void v(String tag, String text, Throwable throwable) {
        if (this.isLoggable(tag, 2)) {
            Log.v(tag, text, throwable);
        }

    }

    public void i(String tag, String text, Throwable throwable) {
        if (this.isLoggable(tag, 4)) {
            Log.i(tag, text, throwable);
        }

    }

    public void w(String tag, String text, Throwable throwable) {
        if (this.isLoggable(tag, 5)) {
            Log.w(tag, text, throwable);
        }

    }

    public void e(String tag, String text, Throwable throwable) {
        if (this.isLoggable(tag, 6)) {
            Log.e(tag, text, throwable);
        }

    }

    public void d(String tag, String text) {
        this.d(tag, text, (Throwable)null);
    }

    public void v(String tag, String text) {
        this.v(tag, text, (Throwable)null);
    }

    public void i(String tag, String text) {
        this.i(tag, text, (Throwable)null);
    }

    public void w(String tag, String text) {
        this.w(tag, text, (Throwable)null);
    }

    public void e(String tag, String text) {
        this.e(tag, text, (Throwable)null);
    }

    public void log(int priority, String tag, String msg) {
        this.log(priority, tag, msg, false);
    }

    public void log(int priority, String tag, String msg, boolean forceLog) {
        if (forceLog || this.isLoggable(tag, priority)) {
            Log.println(priority, tag, msg);
        }

    }
}
