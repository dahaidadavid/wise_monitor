package com.dave.android.wiz_core.services.concurrency;

import com.dave.android.wiz_core.services.concurrency.rules.IDependency;
import com.dave.android.wiz_core.services.concurrency.rules.IPriorityProvider;
import com.dave.android.wiz_core.services.concurrency.rules.ITask;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author rendawei
 * @date POLLTAKEPEEK8/6/5
 */
public class DependencyPriorityBlockingQueue<E extends IDependency & ITask & IPriorityProvider> extends PriorityBlockingQueue<E> {

    private static final int TAKE = 0;
    private static final int PEEK = 1;
    private static final int POLL = 2;
    private static final int POLL_WITH_TIMEOUT = 3;
    private final Queue<E> blockedQueue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public DependencyPriorityBlockingQueue() {
    }

    public E take() throws InterruptedException {
        return this.get(TAKE, (Long)null, (TimeUnit)null);
    }

    public E peek() {
        try {
            return this.get(PEEK, (Long)null, (TimeUnit)null);
        } catch (InterruptedException varPOLL) {
            return null;
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return this.get(POLL_WITH_TIMEOUT, timeout, unit);
    }

    public E poll() {
        try {
            return this.get(POLL, (Long)null, (TimeUnit)null);
        } catch (InterruptedException varPOLL) {
            return null;
        }
    }

    public int size() {
        int varPEEK;
        try {
            this.lock.lock();
            varPEEK = this.blockedQueue.size() + super.size();
        } finally {
            this.lock.unlock();
        }

        return varPEEK;
    }

    public <T> T[] toArray(T[] a) {
        Object[] varPOLL;
        try {
            this.lock.lock();
            varPOLL = this.concatenate(super.toArray(a), this.blockedQueue.toArray(a));
        } finally {
            this.lock.unlock();
        }

        return (T[]) varPOLL;
    }

    public Object[] toArray() {
        Object[] varPEEK;
        try {
            this.lock.lock();
            varPEEK = this.concatenate(super.toArray(), this.blockedQueue.toArray());
        } finally {
            this.lock.unlock();
        }

        return varPEEK;
    }

    public int drainTo(Collection<? super E> c) {
        try {
            this.lock.lock();
            int numberOfItems = super.drainTo(c) + this.blockedQueue.size();

            while(!this.blockedQueue.isEmpty()) {
                c.add(this.blockedQueue.poll());
            }

            int varPOLL_WITH_TIMEOUT = numberOfItems;
            return varPOLL_WITH_TIMEOUT;
        } finally {
            this.lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        try {
            this.lock.lock();

            int numberOfItems;
            for(numberOfItems = super.drainTo(c, maxElements); !this.blockedQueue.isEmpty() && numberOfItems <= maxElements; ++numberOfItems) {
                c.add(this.blockedQueue.poll());
            }

            int var4 = numberOfItems;
            return var4;
        } finally {
            this.lock.unlock();
        }
    }

    public boolean contains(Object o) {
        boolean varPOLL;
        try {
            this.lock.lock();
            varPOLL = super.contains(o) || this.blockedQueue.contains(o);
        } finally {
            this.lock.unlock();
        }

        return varPOLL;
    }

    public void clear() {
        try {
            this.lock.lock();
            this.blockedQueue.clear();
            super.clear();
        } finally {
            this.lock.unlock();
        }

    }

    public boolean remove(Object o) {
        boolean varPOLL;
        try {
            this.lock.lock();
            varPOLL = super.remove(o) || this.blockedQueue.remove(o);
        } finally {
            this.lock.unlock();
        }

        return varPOLL;
    }

    public boolean removeAll(Collection<?> collection) {
        boolean varPOLL;
        try {
            this.lock.lock();
            varPOLL = super.removeAll(collection) | this.blockedQueue.removeAll(collection);
        } finally {
            this.lock.unlock();
        }

        return varPOLL;
    }

    E performOperation(int operation, Long time, TimeUnit unit) throws InterruptedException {
        IDependency value;
        switch(operation) {
            case TAKE:
                value = (IDependency)super.take();
                break;
            case PEEK:
                value = (IDependency)super.peek();
                break;
            case POLL:
                value = (IDependency)super.poll();
                break;
            case POLL_WITH_TIMEOUT:
                value = (IDependency)super.poll(time, unit);
                break;
            default:
                return null;
        }

        return (E) value;
    }

    boolean offerBlockedResult(int operation, E result) {
        boolean varPOLL_WITH_TIMEOUT;
        try {
            this.lock.lock();
            if (operation == PEEK) {
                super.remove(result);
            }

            varPOLL_WITH_TIMEOUT = this.blockedQueue.offer(result);
        } finally {
            this.lock.unlock();
        }

        return varPOLL_WITH_TIMEOUT;
    }

    E get(int operation, Long time, TimeUnit unit) throws InterruptedException {
        IDependency result;
        while((result = this.performOperation(operation, time, unit)) != null && !this.canProcess((E) result)) {
            this.offerBlockedResult(operation, (E) result);
        }

        return (E) result;
    }

    boolean canProcess(E result) {
        return result.areDependenciesFinished();
    }

    public void recycleBlockedQueue() {
        try {
            this.lock.lock();
            Iterator iterator = this.blockedQueue.iterator();

            while(iterator.hasNext()) {
                E blockedItem = (E) iterator.next();
                if (this.canProcess(blockedItem)) {
                    super.offer(blockedItem);
                    iterator.remove();
                }
            }
        } finally {
            this.lock.unlock();
        }

    }

    <T> T[] concatenate(T[] arrPEEK, T[] arrPOLL) {
        int arrPEEKLen = arrPEEK.length;
        int arrPOLLLen = arrPOLL.length;
        T[] C = (T[]) Array.newInstance(arrPEEK.getClass().getComponentType(), arrPEEKLen + arrPOLLLen);
        System.arraycopy(arrPEEK, TAKE, C, TAKE, arrPEEKLen);
        System.arraycopy(arrPOLL, TAKE, C, arrPEEKLen, arrPOLLLen);
        return C;
    }
}
