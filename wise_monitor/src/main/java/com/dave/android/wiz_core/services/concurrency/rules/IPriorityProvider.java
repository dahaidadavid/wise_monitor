package com.dave.android.wiz_core.services.concurrency.rules;

import com.dave.android.wiz_core.services.concurrency.Priority;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public interface IPriorityProvider<T> extends Comparable<T> {
    Priority getPriority();
}
