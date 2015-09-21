package org.allin.enq.model;

/**
 * Created by Santi on 22/07/2015.
 */
public class Group {

    private String name;
    private String _id = "";
    private Integer confirmed_times;
    private Integer confirmed_clients;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConfirmed_times(Integer confirmed_times) {
        this.confirmed_times = confirmed_times;
    }

    public void setConfirmed_clients(Integer confirmed_clients) {
        this.confirmed_clients = confirmed_clients;
    }

    public Integer getEstimatedTime()
    {
        return (confirmed_times / (confirmed_clients == 0 ? 1 : confirmed_clients));
    }
}
