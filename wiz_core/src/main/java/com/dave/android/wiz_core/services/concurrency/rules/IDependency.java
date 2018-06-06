package com.dave.android.wiz_core.services.concurrency.rules;

import java.util.Collection;

/**
 * 依赖管理接口
 *
 * @author rendawei
 * @date 2018/6/5
 */
public interface IDependency<T> {

    /**
     * 添加依赖
     *
     * @param task 依赖的任务
     */
    void addDependency(T task);

    /**
     * 获取所有依赖的任务
     *
     * @return Collection<T>
     */
    Collection<T> getDependencies();

    /**
     * 是否所有的依赖任务都执行完成了
     *
     * @return true 已完成
     */
    boolean areDependenciesFinished();
}
