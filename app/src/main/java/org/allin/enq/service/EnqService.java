package org.allin.enq.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;

import com.google.gson.Gson;

import org.allin.enq.model.Group;
import org.allin.enq.model.Queue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import retrofit.RestAdapter;
import retrofit.RetrofitError;

/**
 * Created by Santi on 20/04/2015.
 */
public class EnqService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new EnqServiceBinder();
    private WifiManager wifiManager;
    private EnqRestApiInfo enqRestApiInfo = new EnqRestApiInfo();
    private EnqServiceListener listener;
    private Properties properties;
    private Gson gson = new Gson();
    private EnqRestApiClient enqRestApiClient;

    @Override
    public IBinder onBind(Intent intent) {

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        loadProperties();

        return mBinder;
    }

    public void setListener(EnqServiceListener listener)
    {
        this.listener = listener;
    }

    public void findGroups() throws RetrofitError {

        new Thread(){

            @Override
            public void run() {

                List<Group> groups = null;
                try {
                    groups = enqRestApiClient.getGroups();
                } catch (RetrofitError e) {
                    listener.OnGroupsNotFound(e);
                    return;
                }

                listener.OnGroupsFound(groups);
            }
        }.start();

    }

    public void checkForEnqServer()
    {
        new Thread() {

            @Override
            public void run() {

                DatagramPacket packet = null;

                try {
                    packet = listenForBroadcastAnnounce();
                } catch (ServerNotPresentException e){
                    listener.OnServerNotFound(e);
                    return;
                }

                enqRestApiInfo = gson.fromJson(new String(packet.getData()).trim(), EnqRestApiInfo.class);

                RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://" + enqRestApiInfo.getAddress() + ":" + enqRestApiInfo.getPort().toString()).build();
                enqRestApiClient = restAdapter.create(EnqRestApiClient.class);

                listener.OnServerFound(enqRestApiInfo);

        }}.start();

    }

    public void enqueueIn(final Group selectedGroup) {

        new Thread() {
            @Override
            public void run() {

                Map<String,String> clientData = new HashMap<String, String>();

                clientData.put("hmac",wifiManager.getConnectionInfo().getMacAddress());
                clientData.put("ip",getDeviceIpAddress());
                Map<String, String> result = null;

                try {
                  result = enqRestApiClient.enqueueIn(selectedGroup.get_id(), clientData);
                } catch (RetrofitError e) {
                    listener.OnServiceException(e);
                    return;
                }

                listener.OnClientEnqueued(result);
            }
        }.start();
    }

    public void waitForServerCall() {
        new Thread() {
            @Override
            public void run() {
                ServerSocket socket = null;

                try {
                    socket = new ServerSocket(3131);
                    socket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getApplicationContext())
                                .setContentTitle("My notification")
                                .setContentText("Hello World!");
// Creates an explicit
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
                mNotificationManager.notify(1234, mBuilder.build());



            }
        }.start();
    }

    @Nullable
    private DatagramPacket listenForBroadcastAnnounce() throws ServerNotPresentException {

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(Integer.valueOf(properties.getProperty("broadcast.port")), InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            socket.setSoTimeout(Integer.valueOf(properties.getProperty("broadcast.timeout")));
        } catch (SocketException | UnknownHostException e) {
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



    private String getDeviceIpAddress() {
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    private void loadProperties() {
        AssetManager assetManager = getApplicationContext().getAssets();

        InputStream inputStream = null;

        try {
            inputStream = assetManager.open("enq.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class EnqServiceBinder extends Binder
    {
        public EnqService getService() {
            return EnqService.this;
        }
    }

    private class ServerNotPresentException extends RuntimeException
    {
        public ServerNotPresentException(Throwable throwable) {
            super(throwable);
        }
    }
}
