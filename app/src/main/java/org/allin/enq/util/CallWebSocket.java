package org.allin.enq.util;

import com.google.gson.Gson;

import org.allin.enq.model.EnqCallInfo;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * Created by santiagocarullo on 9/24/15.
 */
public class CallWebSocket extends WebSocketServer {

    private Gson gson = new Gson();
    private OnCallListener listener;

    public CallWebSocket(int port, OnCallListener listener) {
        super( new InetSocketAddress( port ) );
        this.listener = listener;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        conn.close();
        listener.OnCall(gson.fromJson(message, EnqCallInfo.class));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }
}
