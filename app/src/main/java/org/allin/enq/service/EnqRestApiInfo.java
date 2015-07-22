package org.allin.enq.service;

import org.apache.http.util.ByteArrayBuffer;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by Santi on 21/07/2015.
 */
public class EnqRestApiInfo {

    private String name;
    private String address;
    private Integer port;

    public void fillFromByteArray(byte[] data)
    {
        String info = new String(data).trim();
        String[] parts = info.split("\\|");

        address = parts[0];
        port = Integer.valueOf(parts[1]);
        name = parts[2];

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
}
