package com.example.santi.enq;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import org.apache.http.MethodNotSupportedException;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

/**
 * Created by Santi on 20/04/2015.
 */
public class EnqService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new EnqServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public void findQueues(final QueuesFoundListener listener) throws JSONException, IOException, MethodNotSupportedException {



        new Thread(){

            @Override
            public void run() {
                try {
                    listener.OnQueuesFound(QueueFinder.findQueues());
                } catch (MethodNotSupportedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.start();



    }


    public class EnqServiceBinder extends Binder
    {
        EnqService getService() {
            // Return this instance of EnqService so clients can call public methods
            return EnqService.this;
        }
    }
}
