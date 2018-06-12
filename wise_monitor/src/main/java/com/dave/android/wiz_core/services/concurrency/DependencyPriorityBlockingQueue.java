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
 * @since 2018/6/5
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

    @Override
    public E take() throws InterruptedException {
        return this.get(TAKE, null, null);
    }

    @Override
    public E peek() {
        try {
            return this.get(PEEK, null, null);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return this.get(POLL_WITH_TIMEOUT, timeout, unit);
    }

    @Override
    public E poll() {
        try {
            return this.get(POLL, null, null);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public int size() {
        int size;
        try {
            this.lock.lock();
            size = this.blockedQueue.size() + super.size();
        } finally {
            this.lock.unlock();
        }

        return size;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Object[] array;
        try {
            this.lock.lock();
            array = this.concatenate(super.toArray(a), this.blockedQueue.toArray(a));
        } finally {
            this.lock.unlock();
        }

        return (T[]) array;
    }

    @Override
    public Object[] toArray() {
        Object[] array;
        try {
            this.lock.lock();
            array = this.concatenate(super.toArray(), this.blockedQueue.toArray());
        } finally {
            this.lock.unlock();
        }

        return array;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        try {
            this.lock.lock();
            int numberOfItems = super.drainTo(c) + this.blockedQueue.size();

            while(!this.blockedQueue.isEmpty()) {
                c.add(this.blockedQueue.poll());
            }

            return numberOfItems;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        try {
            this.lock.lock();

            int numberOfItems;
            for(numberOfItems = super.drainTo(c, maxElements); !this.blockedQueue.isEmpty() && numberOfItems <= maxElements; ++numberOfItems) {
                c.add(this.blockedQueue.poll());
            }

            return numberOfItems;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        boolean isContains;
        try {
            this.lock.lock();
            isContains = super.contains(o) || this.blockedQueue.contains(o);
        } finally {
            this.lock.unlock();
        }

        return isContains;
    }

    @Override
    public void clear() {
        try {
            this.lock.lock();
            this.blockedQueue.clear();
            super.clear();
        } finally {
            this.lock.unlock();
        }

    }

    @Override
    public boolean remove(Object o) {
        boolean removeSuc;
        try {
            this.lock.lock();
            removeSuc = super.remove(o) || this.blockedQueue.remove(o);
        } finally {
            this.lock.unlock();
        }

        return removeSuc;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean removeSuc;
        try {
            this.lock.lock();
            removeSuc = super.removeAll(collection) | this.blockedQueue.removeAll(collection);
        } finally {
            this.lock.unlock();
        }

        return removeSuc;
    }

    E performOperation(int operation, Long time, TimeUnit unit) throws InterruptedException {
        IDependency value;
        switch(operation) {
            case TAKE:
                value = super.take();
                break;
            case PEEK:
                value = super.peek();
                break;
            case POLL:
                value = super.poll();
                break;
            case POLL_WITH_TIMEOUT:
                value = super.poll(time, unit);
                break;
            default:
                return null;
        }

        return (E) value;
    }

    boolean offerBlockedResult(int operation, E result) {
        boolean offerSuc;
        try {
            this.lock.lock();
            if (operation == PEEK) {
                super.remove(result);
            }

            offerSuc = this.blockedQueue.offer(result);
        } finally {
            this.lock.unlock();
        }

        return offerSuc;
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
                if (canProcess(blockedItem)) {
                    super.offer(blockedItem);
                    iterator.remove();
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    <T> T[] concatenate(T[] array0, T[] array1) {
        int array0Len = array0.length;
        int array1Len = array1.length;
        T[] C = (T[]) Array.newInstance(array0.getClass().getComponentType(), array0Len + array1Len);
        System.arraycopy(array0, TAKE, C, TAKE, array0Len);
        System.arraycopy(array1, TAKE, C, array0Len, array1Len);
        return C;
    }
}
