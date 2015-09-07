package org.allin.enq.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;


import org.allin.enq.model.Group;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

    public void findGroups() {

         new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                List<Group> groups = null;
                try {
                    groups = enqRestApiClient.getGroups();
                } catch (RetrofitError e) {
                    listener.OnGroupsNotFound(e);
                    return null;
                }

                listener.OnGroupsFound(groups);
                return null;
            }


        }.execute();

    }

    public void checkForEnqServer()
    {
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                DatagramPacket packet = null;

                try {
                    packet = listenForBroadcastAnnounce();
                } catch(ServerNotPresentException e) {
                    listener.OnServerNotFound(e);
                    return null;
                }

                enqRestApiInfo = gson.fromJson(new String(packet.getData()).trim(), EnqRestApiInfo.class);

                enqRestApiClient = new RestAdapter.Builder()
                        .setEndpoint("http://" + enqRestApiInfo.getAddress() + ":" + enqRestApiInfo.getPort().toString())
                        .build().create(EnqRestApiClient.class);

                listener.OnServerFound(enqRestApiInfo);
                return null;

            }
        }.execute();

    }

    public void enqueueIn(final Group selectedGroup) {



        new AsyncTask<Group,Void,Void>() {

            @Override
            protected Void doInBackground(Group... params) {

                Group selectedGroup = params[0];
                Map<String,String> clientData = new HashMap<String, String>();

                clientData.put("hmac", wifiManager.getConnectionInfo().getMacAddress());
                clientData.put("ip",getDeviceIpAddress());
                Map<String, String> result = null;

                try {
                    result = enqRestApiClient.enqueueIn(selectedGroup.get_id(), clientData);
                } catch (RetrofitError e) {
                    listener.OnClientNotEnqueued(e);
                    return null;
                }

                listener.OnClientEnqueued(result);
                return null;

            }
        }.execute(selectedGroup);


    }

    public void waitForServerCall() {
        new Thread() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                Socket socket = null;
                String response = null;


                try {
                    serverSocket = new ServerSocket(3131);
                    socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    byte[] buffer = new byte[in.available()];
                    in.read(buffer);

                    response = new String(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                LinkedTreeMap map = gson.fromJson(response, LinkedTreeMap.class);
                
                listener.OnServerCall(map);




            }
        }.start();
    }

    @Nullable
    private DatagramPacket listenForBroadcastAnnounce() throws ServerNotPresentException {

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(Integer.valueOf(properties.getProperty("broadcast.port")));
            socket.setBroadcast(true);
            socket.setSoTimeout(Integer.valueOf(properties.getProperty("broadcast.timeout")));
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

    private String getStringFromInputStream(InputStream in) throws IOException {
        byte[] buffer = new byte[in.available()];
        in.read(buffer);
        return new String(buffer);
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
