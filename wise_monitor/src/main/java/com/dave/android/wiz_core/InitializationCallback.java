package com.dave.android.wiz_core;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public interface InitializationCallback<T> {

    InitializationCallback EMPTY = new InitializationCallback.Empty();

    void success(T var1);

    void failure(Exception var1);

    class Empty implements InitializationCallback<Object> {

        private Empty() {
        }

        public void success(Object object) {
        }

        public void failure(Exception exception) {
        }
    }
}
