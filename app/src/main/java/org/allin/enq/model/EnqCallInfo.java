package org.allin.enq.model;

import java.io.Serializable;

/**
 * Created by santiagocarullo on 9/17/15.
 */
public class EnqCallInfo implements Serializable {

    private Integer paydesk_number;
    private Integer reenqueue_count;
    private Integer next_estimated_time;

    public Integer getPaydeskNumber() {
        return paydesk_number;
    }

    public void setPaydesk_number(Integer paydesk) {
        this.paydesk_number = paydesk;
    }

    public Integer getReenqueueCount() {
        return reenqueue_count;
    }

    public void setReenqueue_count(Integer reenqueue_count) {
        this.reenqueue_count = reenqueue_count;
    }

    public Integer getNextEstimatedTime() {
        return next_estimated_time;
    }

    public void setNext_estimated_time(Integer next_estimated_time) {
        this.next_estimated_time = next_estimated_time;
    }
}
