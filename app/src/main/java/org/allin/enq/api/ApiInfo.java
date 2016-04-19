package org.allin.enq.api;


/**
 * Created by Santi on 21/07/2015.
 */
public class ApiInfo {

    private String name;
    private String address;
    private Integer port;
    private Integer reenqueue_limit;
    private Integer call_timeout;

    public void fillFromByteArray(byte[] data)
    {
        String info = new String(data).trim();
        String[] parts = info.split("\\|");

        address = parts[0];
        port = Integer.valueOf(parts[1]);
        name = parts[2];
        reenqueue_limit = Integer.valueOf(parts[1]);

    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getReenqueueLimit() { return reenqueue_limit; }

    public Integer getCallTimeout() { return call_timeout; }
}
