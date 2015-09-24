package org.allin.enq.model;

/**
 * Created by santiagocarullo on 9/17/15.
 */
public class ClientEnqueuedInfo {

    private String client_id;
    private Integer client_number;
    private Integer estimated_time;

    public String getClientId() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public Integer getClientNumber() {
        return client_number;
    }

    public void setClient_number(Integer client_number) {
        this.client_number = client_number;
    }

    public Integer getEstimatedTime() {
        return estimated_time;
    }

    public void setEstimated_time(Integer estimated_time) {
        this.estimated_time = estimated_time;
    }
}
