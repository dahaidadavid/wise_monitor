package com.dave.android.wiz_core.services.concurrency.rules;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public interface IDelegateProvider {

    <T extends IDependency<ITask> & IPriorityProvider & ITask> T getDelegate();
}
