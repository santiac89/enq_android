package org.allin.enq.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;

import org.allin.enq.R;
import org.allin.enq.activity.CallReceivedActivity;
import org.allin.enq.activity.WaitingActivity;
import org.allin.enq.api.ApiClient;
import org.allin.enq.api.ApiInfo;
import org.allin.enq.model.Group;
import org.allin.enq.util.EnqProperties;
import org.allin.enq.util.NetworkUtils;

import java.io.IOException;
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

    private ApiClient apiClient;
    private ApiInfo apiInfo;
    private CallInfo callInfo;
    private ClientInfo clientInfo;
    private CallManager callManager = new CallManager();


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
                    serviceListener.OnGroupsNotFound();
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
    public void findService()
    {
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... params) {

            try {
                NetworkUtils.sendBroadcastMessage(getApplicationContext(),"whereareyou?", 6000);
            } catch (SocketException e) {
                serviceListener.OnServerNotFound(e);
                return null;
            } catch (IOException e) {
                serviceListener.OnServerNotFound(e);
                return null;
            }

            String response;
            try {
                response = NetworkUtils.receiveSingleTCPMessage(6001, 5000);
            } catch (IOException e) {
                serviceListener.OnServerNotFound(e);
                return null;
            }

            apiInfo = gson.fromJson(response, ApiInfo.class);

            apiClient = new RestAdapter.Builder()
                    .setEndpoint("http://" + apiInfo.getAddress() + ":" + apiInfo.getPort().toString())
                    .build().create(ApiClient.class);

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
        callManager.setCallback(new CallReceivedCallback() {
            @Override
            public void call(CallInfo info) {
                callInfo = info;
                Intent callIntent = new Intent(getApplicationContext(), CallReceivedActivity.class);
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(callIntent);
                stopForeground(true);
            }
        });
        callManager.start(3131);
        startInForeground();
    }

    public void cancelWaitingForCall() {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... params) {
            try {
                apiClient.cancel(clientInfo.getClientId());
                callManager.stop();
            } catch (RetrofitError e) {
                e.printStackTrace();
            } finally {
                stopForeground(true);
            }

            return null;
            }
        }.execute();

    }

    /**
     * Sends a message to the server using the opened TCP Socket
     * @param response The message to send
     */
    public void sendCallResponse(final String response) {
        callManager.respond(response);
    }

    public Boolean isWaitingForServerCall() {
        return callManager.isWaiting();
    }

    public Integer getCallTimeout() { return apiInfo.getCallTimeout(); }

    public Integer getPaydeskNumber() { return callInfo.getPaydeskNumber(); }

    public String getNextEstimatedTime() { return callInfo.getNextEstimatedTime().toString(); }

    public Boolean clientReachedReenqueueLimit() { return callInfo.getReenqueueCount() >= apiInfo.getReenqueueLimit(); }

    public Integer getPaydeskArrivalTimeout() { return clientInfo.getPaydeskArrivalTimeout(); }

    public Integer getClientNumber() { return clientInfo.getClientNumber(); }

    public String getGroupName() {
        return clientInfo.getGroupName();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelWaitingForCall();
        stopForeground(true);
    }
}
