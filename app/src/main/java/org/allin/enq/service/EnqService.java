package org.allin.enq.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
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
    private boolean isBusy = false;
    private Gson gson = new Gson();
    private EnqRestApiClient enqRestApiClient;

    @Override
    public IBinder onBind(Intent intent) {

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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


        return mBinder;
    }

    public void setListener(EnqServiceListener listener)
    {
        this.listener = listener;
    }

    public boolean isBusy()
    {
        return isBusy;
    }

    public void findQueues() {

        new Thread(){

            @Override
            public void run() {

                isBusy = true;
                List<Group> groups = null;

                try {
                    groups = enqRestApiClient.getGroups();
                } catch (RetrofitError e) {
                    return;
                }

               isBusy = false;
               listener.OnGroupsFound(groups);


            }
        }.start();

    }


    public boolean isWifiEnabled()
    {
        return wifiManager.isWifiEnabled();
    }

    public void checkForEnqService()
    {
        new Thread() {

            @Override
            public void run() {

                isBusy = true;
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(Integer.valueOf(properties.getProperty("broadcast.port")), InetAddress.getByName("0.0.0.0"));
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                try {
                    socket.setBroadcast(true);
                } catch (SocketException e) {
                    e.printStackTrace();
                }

                byte[] data = new byte[1024];

                DatagramPacket packet = new DatagramPacket(data, data.length);

                try {
                    socket.setSoTimeout(Integer.valueOf(properties.getProperty("broadcast.timeout")));
                } catch (SocketException e) {
                    e.printStackTrace();
                }

                try {
                    socket.receive(packet);
                } catch (IOException e) {

                    socket.close();
                   if (e instanceof InterruptedIOException)
                   {
                       isBusy = false;
                       listener.OnServiceNotFound();
                       return;
                   }


                }

                socket.close();

                enqRestApiInfo = gson.fromJson(new String(packet.getData()).trim(),EnqRestApiInfo.class);

                isBusy = false;

                RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://" + enqRestApiInfo.getAddress() + ":" + enqRestApiInfo.getPort().toString()).build();
                enqRestApiClient = restAdapter.create(EnqRestApiClient.class);


                listener.OnServiceFound(enqRestApiInfo);

        }}.start();

    }

    public void enqueueIn(final Group selectedGroup) {

        new Thread() {
            @Override
            public void run() {

                Map<String,String> clientData = new HashMap<String, String>();
                clientData.put("hmac",wifiManager.getConnectionInfo().getMacAddress());

                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
                clientData.put("ip",ip);

                Map<String,String> result = enqRestApiClient.enqueueIn(selectedGroup.get_id(),clientData);

                listener.OnClientEnqueued(result);
            }
        }.start();
}


    public class EnqServiceBinder extends Binder
    {
        public EnqService getService() {
            return EnqService.this;
        }
    }
}
