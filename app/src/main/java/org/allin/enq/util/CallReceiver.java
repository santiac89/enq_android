package org.allin.enq.util;

import android.os.AsyncTask;

import com.google.gson.Gson;
import org.allin.enq.model.EnqCallInfo;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by santiagocarullo on 10/25/15.
 */
public class CallReceiver {

    EnqCallInfo callInfo;
    ServerSocket serverSocket;
    BufferedWriter socketWriter;
    Boolean waitingForCall = false;
    Gson gson = new Gson();
    CallReceivedCallback callback;

    public void setCallback(CallReceivedCallback callback) {
        this.callback = callback;
    }
    /**
     * Opens a TCP socket in this device to wait for the call from the server
     */
    public void start(final Integer port) {

        callInfo = null;

        new Thread() {
            @Override
            public void run() {

            String response = null;

            try {

                if (serverSocket != null) {
                    serverSocket.close();
                }

                serverSocket = new ServerSocket(port);
                waitingForCall = true;
                Socket socket = serverSocket.accept();
                serverSocket.close();
                waitingForCall = false;
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                while (response == null) response = socketReader.readLine();

            } catch (IOException e) {
                waitingForCall = false;
                return;
            }

            callInfo = gson.fromJson(response, EnqCallInfo.class);

            callback.call(callInfo);

            }
        }.start();
    }

    public void stop() {
        try {
            serverSocket.close();
            socketWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void respond(final String message) {

        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    socketWriter.write(message);
                    socketWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }

    public Boolean isWaiting() {
        return waitingForCall;
    }
}
