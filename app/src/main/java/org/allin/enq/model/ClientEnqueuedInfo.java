package org.allin.enq.model;

/**
 * Created by santiagocarullo on 9/17/15.
 */
public class ClientEnqueuedInfo {

    private String client_id;
    private Integer client_number;
    private Integer paydesk_arrival_timeout;
    private String group_name;

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

    public Integer getPaydeskArrivalTimeout() {
        return paydesk_arrival_timeout;
    }

    public void setPaydesk_arrival_timeout(Integer paydesk_arrival_timeout) {
        this.paydesk_arrival_timeout = paydesk_arrival_timeout;
    }

    public void setGroup_name(String name) {
        group_name = name;
    }

    public String getGroupName() {
        return group_name;
    }
}
