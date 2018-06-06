package com.dave.android.wiz_core.services.concurrency;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public class UnmetDependencyException extends RuntimeException {

    public UnmetDependencyException() {
    }

    public UnmetDependencyException(String detailMessage) {
        super(detailMessage);
    }

    public UnmetDependencyException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UnmetDependencyException(Throwable throwable) {
        super(throwable);
    }
}
