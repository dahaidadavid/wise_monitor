package com.dave.android.wiz_core.services.concurrency;

import com.dave.android.wiz_core.services.concurrency.rules.IPriorityProvider;

/**
 * @author rendawei
 * @date 2018/6/5
 */
public enum Priority {
    LOW,
    NORMAL,
    HIGH,
    IMMEDIATE;

    static <Y> int compareTo(IPriorityProvider self, Y other) {
        Priority otherPriority;
        if (other instanceof IPriorityProvider) {
            otherPriority = ((IPriorityProvider) other).getPriority();
        } else {
            otherPriority = NORMAL;
        }

        return otherPriority.ordinal() - self.getPriority().ordinal();
    }
}
