package com.dave.android.wiz_core;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public class InitializationException extends RuntimeException {

    public InitializationException(String detailMessage) {
        super(detailMessage);
    }

    public InitializationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
