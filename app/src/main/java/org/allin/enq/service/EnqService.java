package org.allin.enq.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;

import org.allin.enq.R;
import org.allin.enq.activity.CallReceivedActivity;
import org.allin.enq.activity.WaitingActivity;
import org.allin.enq.model.EnqClientInfo;
import org.allin.enq.model.EnqCallInfo;
import org.allin.enq.api.EnqApiClient;
import org.allin.enq.model.EnqApiInfo;
import org.allin.enq.model.Group;
import org.allin.enq.util.EnqProperties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit.RestAdapter;
import retrofit.RetrofitError;


/**
 * Created by Santi on 20/04/2015.
 */
public class EnqService extends Service {

    private int FOREGROUND_SERVICE_ID = 138;

    private final IBinder serviceBinder = new EnqServiceBinder();
    private WifiManager wifiManager;
    private EnqServiceListener serviceListener;
    private Gson gson = new Gson();
    private ServerSocket serverSocket = null;
    private BufferedWriter socketWriter = null;

    private Boolean isWaitingForServerCall = false;

    private EnqApiClient apiClient;
    private EnqApiInfo apiInfo;
    private EnqCallInfo callInfo;
    private EnqClientInfo clientInfo;


    /**
     * Finds groups in the found server
     */
    public void findGroups() {

         new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                List<Group> groups = null;
                try {
                    groups = apiClient.getGroups();
                } catch (RetrofitError e) {
                    serviceListener.OnGroupsNotFound(e);
                    return null;
                }

                serviceListener.OnGroupsFound(groups);
                return null;
            }


        }.execute();

    }

    /**
     *  Senses the network for broadcast messages from the server and retrieves information about it
     */
    public void checkForServer()
    {
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                DatagramPacket packet;

                try {
                    packet = receiveBroadcastMessage();
                } catch(ServerNotPresentException e) {
                    serviceListener.OnServerNotFound(e);
                    return null;
                }

                apiInfo = gson.fromJson(new String(packet.getData()).trim(), EnqApiInfo.class);

                apiClient = new RestAdapter.Builder()
                        .setEndpoint("http://" + apiInfo.getAddress() + ":" + apiInfo.getPort().toString())
                        .build().create(EnqApiClient.class);

                serviceListener.OnServerFound();
                return null;

            }
        }.execute();
    }

    /**
     * Enqueues this device in the desired group
     * @param selectedGroup The group where the device will be enqueued
     */
    public void enqueueInGroup(final Group selectedGroup) {

        new AsyncTask<Group,Void,Void>() {

            @Override
            protected Void doInBackground(Group... params) {

            Group selectedGroup = params[0];
            Map<String,String> clientData = new HashMap<String, String>();

            clientData.put("hmac", wifiManager.getConnectionInfo().getMacAddress());
            clientData.put("ip", getDeviceIpAddress());

            try {
                clientInfo = apiClient.enqueueIn(selectedGroup.get_id(), clientData);
            } catch (RetrofitError e) {
                serviceListener.OnClientNotEnqueued(e);
                return null;
            }

            serviceListener.OnClientEnqueued();
            return null;

            }
        }.execute(selectedGroup);
    }

    /**
     * Opens a TCP socket in this device to wait for the call from the server
     */
    public void startWaitingForCall() {

        callInfo = null;

        new Thread() {
            @Override
            public void run() {

                String response = null;

                try {

                    if (serverSocket != null) {
                        serverSocket.close();
                    }

                    serverSocket = new ServerSocket(3131);
                    isWaitingForServerCall = true;
                    startInForeground();
                    Socket socket = serverSocket.accept();
                    serverSocket.close();
                    isWaitingForServerCall = false;
                    stopForeground(true);
                    BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    while (response == null) response = socketReader.readLine();

                } catch (IOException e) {
                    isWaitingForServerCall = false;
                    return;
                }

                callInfo = gson.fromJson(response, EnqCallInfo.class);

                Intent callIntent = new Intent(getApplicationContext(), CallReceivedActivity.class);
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(callIntent);

            }
        }.start();
    }

    public void cancelWaiting() {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    apiClient.cancel(clientInfo.getClientId());
                    serverSocket.close();
                    socketWriter.close();
                } catch (RetrofitError e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                stopForeground(true);
                return null;
            }
        }.execute();

    }

    /**
     * Sends a message to the server using the opened TCP Socket
     * @param response The message to send
     */
    public void sendCallResponse(String response) {
        try {
            socketWriter.write(response);
            socketWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method puts this device to listen for broadcast messages from the server in the port
     * specified by the configuration
     *
     * @return A DatagramPacket with data from the broadcast
     * @throws ServerNotPresentException When no message arrived passed certain time
     */
    @Nullable
    private DatagramPacket receiveBroadcastMessage() throws ServerNotPresentException {

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(Integer.valueOf(EnqProperties.getProperty("broadcast.port")));
            socket.setBroadcast(true);
            socket.setSoTimeout(Integer.valueOf(EnqProperties.getProperty("broadcast.timeout")));
        } catch (SocketException e) {
            socket.close();
            throw new ServerNotPresentException(e);
        }

        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

        try {
            socket.receive(packet);
            socket.close();
        } catch (IOException e) {
           socket.close();
           if (e instanceof InterruptedIOException) {
               throw new ServerNotPresentException(e);
           }
        }

        return packet;
    }

    public Boolean isWaitingForServerCall() {
        return isWaitingForServerCall;
    }

    public Integer getCallTimeout() {
        return apiInfo.getCallTimeout();
    }

    public Integer getClientNumber() {
        return clientInfo.getClientNumber();
    }

    public Integer getPaydeskNumber() {
        return callInfo.getPaydeskNumber();
    }

    public Boolean clientReachedReenqueueLimit() {
         return callInfo.getReenqueueCount() >= apiInfo.getReenqueueLimit() ;
    }

    private String getDeviceIpAddress() {
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    public int getPaydeskArrivalTimeout() {
        return clientInfo.getPaydeskArrivalTimeout();
    }

    public String getGroupName() {
        return clientInfo.getGroupName();
    }

    public String getNextEstimatedTime() {
        return callInfo.getNextEstimatedTime().toString();
    }

    /**
     * Exception thrown when no server could be found in the network
     */
    private class ServerNotPresentException extends RuntimeException
    {
        public ServerNotPresentException(Throwable throwable) {
            super(throwable);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void startInForeground() {

        Intent intent = new Intent(getApplicationContext(),WaitingActivity.class);
        PendingIntent watitingActivityIntent = PendingIntent.getActivity(
            getApplicationContext(),
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getBaseContext());

        notificationBuilder
            .setSmallIcon(R.drawable.icon_bw)
            .setContentTitle("EnQ")
            .setContentText("Esperando llamado...")
            .setContentIntent(watitingActivityIntent);

        startForeground(FOREGROUND_SERVICE_ID, notificationBuilder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        EnqProperties.load(getApplicationContext());
        return serviceBinder;
    }

    /**
     * Binder returned to the client of this service when this service is asked to be started
     */
    public class EnqServiceBinder extends Binder
    {
        public EnqService getService() {
            return EnqService.this;
        }
    }

    public void setListener(EnqServiceListener listener) {
        this.serviceListener = listener;
    }

}
