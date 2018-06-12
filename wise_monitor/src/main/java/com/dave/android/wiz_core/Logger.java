package com.dave.android.wiz_core;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public interface Logger {
    boolean isLoggable(String var1, int var2);

    int getLogLevel();

    void setLogLevel(int var1);

    void d(String var1, String var2, Throwable var3);

    void v(String var1, String var2, Throwable var3);

    void i(String var1, String var2, Throwable var3);

    void w(String var1, String var2, Throwable var3);

    void e(String var1, String var2, Throwable var3);

    void d(String var1, String var2);

    void v(String var1, String var2);

    void i(String var1, String var2);

    void w(String var1, String var2);

    void e(String var1, String var2);

    void log(int var1, String var2, String var3);

    void log(int var1, String var2, String var3, boolean var4);
}
